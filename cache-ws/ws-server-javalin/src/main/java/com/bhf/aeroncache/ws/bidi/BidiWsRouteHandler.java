package com.bhf.aeroncache.ws.bidi;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CacheStats;
import com.bhf.aeroncache.models.results.CacheStatsResult;
import com.bhf.aeroncache.models.results.CancelItemRemovalResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.DecrementCounterResult;
import com.bhf.aeroncache.models.results.DeleteCacheResult;
import com.bhf.aeroncache.models.results.GetAllCacheEntriesResult;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.models.results.IncrementCounterResult;
import com.bhf.aeroncache.models.results.PatchValueResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.models.results.SetCounterResult;
import com.bhf.aeroncache.ws.application.CacheSubscriptionRequestPublisher;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import com.bhf.aeroncache.ws.bidi.messages.BidiClientMessage;
import com.bhf.aeroncache.ws.bidi.messages.BidiCommand;
import com.bhf.aeroncache.ws.bidi.messages.BidiCommandResponse;
import com.bhf.aeroncache.ws.bidi.messages.BidiEntries;
import com.bhf.aeroncache.ws.bidi.messages.BidiError;
import com.bhf.aeroncache.ws.bidi.messages.BidiStats;
import com.bhf.aeroncache.ws.bidi.messages.BidiStreamUpdate;
import com.bhf.aeroncache.ws.bidi.messages.BidiSubscribe;
import com.bhf.aeroncache.ws.bidi.messages.BidiSubscribeAck;
import com.bhf.aeroncache.ws.bidi.messages.BidiUnsubscribe;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsMessageContext;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Route handler for the bidirectional websocket endpoint.
 *
 * <p>The websocket counterpart of the Aeron gateway's {@code GatewayIngressAgent}: a single connection
 * carries the full cache/counter command surface plus dynamic subscribe/unsubscribe, correlated by a
 * client-supplied {@code correlationId}. Inbound JSON frames are decoded by {@link BidiMessageCodec} and
 * dispatched onto the shared {@link CacheSubscriptionRequestPublisher} instances (one for regular caches,
 * one for counters) &mdash; the same publishers the one-directional subscription routes use. Responses,
 * streamed entries/stats and streaming updates are written back over the same connection.</p>
 *
 * <p>Command result callbacks fire on the cluster egress listener thread, so all sends for a connection
 * are funnelled through a per-connection {@link BidiSession} whose {@code send} is synchronized, guarding
 * against a race with an inline error sent from the websocket receive thread.</p>
 */
@Log4j2
@SuppressWarnings({"unchecked", "rawtypes"})
public class BidiWsRouteHandler {

    private final CacheSubscriptionRequestPublisher cacheSubs;
    private final CacheSubscriptionRequestPublisher countersSubs;
    private final BidiMessageCodec codec = new BidiMessageCodec();
    private final Map<String, BidiSession> sessions = new ConcurrentHashMap<>();

    public BidiWsRouteHandler(CacheSubscriptionRequestPublisher cacheSubs,
                              CacheSubscriptionRequestPublisher countersSubs) {
        this.cacheSubs = cacheSubs;
        this.countersSubs = countersSubs;
    }

    /**
     * Wire the websocket lifecycle callbacks for the bidirectional endpoint.
     *
     * @param wsConfig the websocket config.
     */
    public void handleBidi(WsConfig wsConfig) {
        wsConfig.onConnect(this::onConnect);
        wsConfig.onMessage(this::onMessage);
        wsConfig.onClose(this::onClose);
        wsConfig.onError(this::onError);
    }

    private void onConnect(WsConnectContext ctx) {
        ctx.enableAutomaticPings();
        sessions.put(ctx.sessionId(), new BidiSession(ctx));
        log.info("BIDI websocket connected, sessionId: {}", ctx.sessionId());
    }

    private void onMessage(WsMessageContext ctx) {
        var session = sessions.computeIfAbsent(ctx.sessionId(), id -> new BidiSession(ctx));
        BidiClientMessage message;
        try {
            message = codec.parse(ctx.message());
        } catch (BidiProtocolException e) {
            log.warn("Rejecting malformed BIDI frame on sessionId {}: {}", ctx.sessionId(), e.getMessage());
            session.send(codec.write(BidiError.of(null, CacheOperationStatus.ERROR, e.getMessage())));
            return;
        }

        try {
            switch (message) {
                case BidiCommand command -> handleCommand(session, command);
                case BidiSubscribe subscribe -> handleSubscribe(session, subscribe);
                case BidiUnsubscribe unsubscribe -> handleUnsubscribe(session, unsubscribe);
            }
        } catch (Exception e) {
            log.warn("Error handling BIDI frame on sessionId {}", ctx.sessionId(), e);
            session.send(codec.write(BidiError.of(message.correlationId(), CacheOperationStatus.ERROR, e.getMessage())));
        }
    }

    private void onClose(WsCloseContext ctx) {
        log.info("BIDI websocket closed for sessionId: {}", ctx.sessionId());
        sessions.remove(ctx.sessionId());
        // Mirror AbstractWsRouteHandlers: subscription teardown to the cluster only applies in clustered mode.
        if (WebsocketApplication.isClusteredMode()) {
            var requestId = UUID.randomUUID().toString();
            cacheSubs.handleWsClosed(WebsocketApplication.getCache(), requestId, ctx.sessionId());
            countersSubs.handleWsClosed(WebsocketApplication.getCache(), requestId, ctx.sessionId());
        }
    }

    private void onError(WsErrorContext ctx) {
        log.warn("BIDI websocket error on sessionId: {}", ctx.sessionId(), ctx.error());
        sessions.remove(ctx.sessionId());
        var requestId = UUID.randomUUID().toString();
        cacheSubs.handleWsError(WebsocketApplication.getCache(), requestId, ctx.sessionId());
        countersSubs.handleWsError(WebsocketApplication.getCache(), requestId, ctx.sessionId());
    }

    // ------------------------------------------------------------------ commands

    private void handleCommand(BidiSession session, BidiCommand command) {
        final String correlationId = requestId(command.correlationId());
        final String cacheId = command.cacheId();
        final String key = command.key();
        final String value = command.value();
        final long ttl = command.ttl();
        final long counterValue = command.counterValue();

        switch (command.op()) {
            case CREATE_CACHE -> cacheSubs.sendCreateCache(correlationId, cacheId,
                    (Consumer<CreateCacheResult>) o -> respondStatus(session, correlationId, ((CreateCacheResult) o).getStatus(), cacheId));
            case ADD_CACHE_ENTRY -> cacheSubs.addCacheEntry(correlationId, cacheId, key, value, ttl,
                    (Consumer<AddCacheEntryResult>) o -> respondStatus(session, correlationId, ((AddCacheEntryResult) o).getStatus(), cacheId, key, null));
            case PATCH_CACHE_ENTRY -> cacheSubs.patchValue(correlationId, cacheId, key, value,
                    (Consumer<PatchValueResult>) o -> respondStatus(session, correlationId, ((PatchValueResult) o).getStatus(), cacheId, key, null));
            case GET_CACHE_ENTRY -> cacheSubs.getCacheEntry(correlationId, cacheId, key,
                    (Consumer<GetCacheEntryResult>) o -> respondEntry(session, correlationId, (GetCacheEntryResult) o));
            case CLEAR_CACHE -> cacheSubs.clearCache(correlationId, cacheId,
                    (Consumer<ClearCacheResult>) o -> respondStatus(session, correlationId, ((ClearCacheResult) o).getStatus(), cacheId));
            case DELETE_CACHE -> cacheSubs.deleteCache(correlationId, cacheId,
                    (Consumer<DeleteCacheResult>) o -> respondStatus(session, correlationId, ((DeleteCacheResult) o).getStatus(), cacheId));
            case REMOVE_CACHE_ENTRY -> cacheSubs.removeCacheEntry(correlationId, cacheId, key,
                    (Consumer<RemoveCacheEntryResult>) o -> respondStatus(session, correlationId, ((RemoveCacheEntryResult) o).getStatus(), cacheId, key, null));
            case CANCEL_CACHE_ITEM_REMOVAL -> cacheSubs.cancelItemRemoval(correlationId, cacheId, key,
                    (Consumer<CancelItemRemovalResult>) o -> respondStatus(session, correlationId, ((CancelItemRemovalResult) o).getStatus(), cacheId, key, null));
            case GET_CACHE_ENTRIES -> cacheSubs.getCacheEntries(correlationId, cacheId,
                    (Consumer<GetAllCacheEntriesResult>) o -> streamEntries(session, correlationId, (GetAllCacheEntriesResult) o));
            case GET_CACHE_STATS -> cacheSubs.getAllCacheStats(correlationId,
                    (Consumer<CacheStatsResult>) o -> streamStats(session, correlationId, (CacheStatsResult) o));

            case CREATE_COUNTER_CACHE -> countersSubs.sendCreateCache(correlationId, cacheId,
                    (Consumer<CreateCacheResult>) o -> respondStatus(session, correlationId, ((CreateCacheResult) o).getStatus(), cacheId));
            case ADD_COUNTER_ENTRY -> countersSubs.addCacheEntry(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<AddCacheEntryResult>) o -> respondStatus(session, correlationId, ((AddCacheEntryResult) o).getStatus(), cacheId, key, null));
            case GET_COUNTER_ENTRY -> countersSubs.getCacheEntry(correlationId, cacheId, key,
                    (Consumer<GetCacheEntryResult>) o -> respondEntry(session, correlationId, (GetCacheEntryResult) o));
            case CLEAR_COUNTER_CACHE -> countersSubs.clearCache(correlationId, cacheId,
                    (Consumer<ClearCacheResult>) o -> respondStatus(session, correlationId, ((ClearCacheResult) o).getStatus(), cacheId));
            case DELETE_COUNTER_CACHE -> countersSubs.deleteCache(correlationId, cacheId,
                    (Consumer<DeleteCacheResult>) o -> respondStatus(session, correlationId, ((DeleteCacheResult) o).getStatus(), cacheId));
            case REMOVE_COUNTER_ENTRY -> countersSubs.removeCacheEntry(correlationId, cacheId, key,
                    (Consumer<RemoveCacheEntryResult>) o -> respondStatus(session, correlationId, ((RemoveCacheEntryResult) o).getStatus(), cacheId, key, null));
            case CANCEL_COUNTER_ITEM_REMOVAL -> countersSubs.cancelItemRemoval(correlationId, cacheId, key,
                    (Consumer<CancelItemRemovalResult>) o -> respondStatus(session, correlationId, ((CancelItemRemovalResult) o).getStatus(), cacheId, key, null));
            case GET_COUNTER_ENTRIES -> countersSubs.getCacheEntries(correlationId, cacheId,
                    (Consumer<GetAllCacheEntriesResult>) o -> streamEntries(session, correlationId, (GetAllCacheEntriesResult) o));
            case GET_COUNTER_STATS -> countersSubs.getAllCacheStats(correlationId,
                    (Consumer<CacheStatsResult>) o -> streamStats(session, correlationId, (CacheStatsResult) o));
            case INCREMENT_COUNTER_ENTRY -> countersSubs.incrementCounter(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<IncrementCounterResult>) o -> respondCounter(session, correlationId, ((IncrementCounterResult) o).getStatus(), cacheId, key, ((IncrementCounterResult) o).getCounterValue()));
            case DECREMENT_COUNTER_ENTRY -> countersSubs.decrementCounter(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<DecrementCounterResult>) o -> respondCounter(session, correlationId, ((DecrementCounterResult) o).getStatus(), cacheId, key, ((DecrementCounterResult) o).getCounterValue()));
            case SET_COUNTER_ENTRY -> countersSubs.setCounter(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<SetCounterResult>) o -> respondCounter(session, correlationId, ((SetCounterResult) o).getStatus(), cacheId, key, ((SetCounterResult) o).getCounterValue()));

            default -> {
                log.warn("Unhandled BIDI op {} on sessionId {}", command.op(), session.sessionId());
                session.send(codec.write(BidiError.of(correlationId, CacheOperationStatus.ERROR, "Unhandled op " + command.op())));
            }
        }
    }

    // ------------------------------------------------------------------ subscriptions

    private void handleSubscribe(BidiSession session, BidiSubscribe subscribe) {
        final boolean counters = subscribe.counters();
        final boolean sendSnapshot = subscribe.sendSnapshot();
        final String correlationId = requestId(subscribe.correlationId());

        final List<String> cacheIds = new ArrayList<>();
        final List<String> keys = new ArrayList<>();
        SubscriptionMode mode = SubscriptionMode.FULL;
        var selectors = subscribe.caches() == null ? List.<BidiSubscribe.CacheSelector>of() : subscribe.caches();
        for (var selector : selectors) {
            cacheIds.add(selector.cacheId());
            var key = selector.key();
            keys.add(key == null || key.isEmpty() ? null : key);
            if (selector.mode() == SubscriptionMode.PATCH) {
                mode = SubscriptionMode.PATCH;
            }
        }

        final CacheSubscriptionRequestPublisher publisher = counters ? countersSubs : cacheSubs;
        final Consumer<Void> failureHandler = ignored ->
                session.send(codec.write(BidiError.of(correlationId, CacheOperationStatus.ERROR, "Subscription failed")));
        final Runnable ackHandler = () ->
                session.send(codec.write(BidiSubscribeAck.of(correlationId, cacheIds)));
        final Consumer<CacheUpdateEvent> updateConsumer = event ->
                session.send(codec.write(BidiStreamUpdate.from(event)));

        log.info("BIDI subscribe session {}, caches {}, keys {}, mode {}, snapshot {}, counters {}",
                session.sessionId(), cacheIds, keys, mode, sendSnapshot, counters);
        publisher.subscribeToCache(WebsocketApplication.getCache(), failureHandler, ackHandler, cacheIds, keys, mode,
                session.sessionId(), correlationId, sendSnapshot, updateConsumer);
    }

    private void handleUnsubscribe(BidiSession session, BidiUnsubscribe unsubscribe) {
        final boolean counters = unsubscribe.counters();
        final String correlationId = requestId(unsubscribe.correlationId());
        final String cacheId = unsubscribe.cacheId();

        final CacheSubscriptionRequestPublisher publisher = counters ? countersSubs : cacheSubs;
        log.info("BIDI unsubscribe session {}, cache {}, counters {}", session.sessionId(), cacheId, counters);
        publisher.unsubscribeSession(WebsocketApplication.getCache(), correlationId, session.sessionId(), cacheId);
    }

    // ------------------------------------------------------------------ response helpers

    private void respondStatus(BidiSession session, String correlationId, CacheOperationStatus status, String cacheId) {
        respondStatus(session, correlationId, status, cacheId, null, null);
    }

    private void respondStatus(BidiSession session, String correlationId, CacheOperationStatus status,
                               String cacheId, String key, String value) {
        session.send(codec.write(BidiCommandResponse.of(correlationId, status, cacheId, key, value)));
    }

    private void respondEntry(BidiSession session, String correlationId, GetCacheEntryResult result) {
        final String cacheId = String.valueOf(result.getCacheId().value());
        final String key = result.getEntryKey() == null ? null : String.valueOf(result.getEntryKey().value());
        final String value = result.getEntryValue() == null ? null : String.valueOf(result.getEntryValue().value());
        session.send(codec.write(BidiCommandResponse.of(correlationId, result.getStatus(), cacheId, key, value)));
    }

    private void respondCounter(BidiSession session, String correlationId, CacheOperationStatus status,
                                String cacheId, String key, long counterValue) {
        session.send(codec.write(BidiCommandResponse.of(correlationId, status, cacheId, key, Long.toString(counterValue))));
    }

    /**
     * Forward a batch of cache/counter entries to the client. The cluster streams entries in one or more
     * batches (mirroring {@code AllCacheEntriesResult}); this fires once per batch and forwards the batch's
     * items together with the {@code endOfBatch} flag so the client can detect completion.
     */
    private void streamEntries(BidiSession session, String correlationId, GetAllCacheEntriesResult result) {
        final String cacheId = String.valueOf(result.getCacheId().value());
        final Map<String, String> items = new HashMap<>();
        result.getValues().forEach((k, v) -> items.put(
                String.valueOf(((Reusable) k).value()),
                v == null ? null : String.valueOf(((Reusable) v).value())));
        session.send(codec.write(BidiEntries.of(correlationId, result.getStatus(), cacheId, items, result.isEndOfBatch())));
    }

    /**
     * Forward cache stats to the client. The cluster delivers all stats in a single grouped
     * {@code AllCacheStatsResult} frame, so this is streamed as one end-of-batch stats frame.
     */
    private void streamStats(BidiSession session, String correlationId, CacheStatsResult result) {
        final List<BidiStats.StatEntry> stats = new ArrayList<>();
        for (Object o : result.getStats()) {
            final CacheStats stat = (CacheStats) o;
            stats.add(new BidiStats.StatEntry(
                    String.valueOf(stat.getCacheId().value()),
                    stat.addedCount, stat.removedCount, stat.clearedCount, stat.size));
        }
        session.send(codec.write(BidiStats.of(correlationId, result.getOperationStatus(), stats, true)));
    }

    private static String requestId(String correlationId) {
        return (correlationId == null || correlationId.isEmpty()) ? UUID.randomUUID().toString() : correlationId;
    }

    /**
     * A connected client session: the return path for one websocket connection.
     *
     *
     * <p>Sends are synchronized because command result callbacks arrive on the cluster egress thread while
     * an inline protocol error may be sent from the websocket receive thread.</p>
     */
    private static final class BidiSession {
        private final WsContext ctx;
        private final String sessionId;
        private final Object sendLock = new Object();

        BidiSession(WsContext ctx) {
            this.ctx = ctx;
            this.sessionId = ctx.sessionId();
        }

        String sessionId() {
            return sessionId;
        }

        void send(String json) {
            synchronized (sendLock) {
                try {
                    ctx.send(json);
                } catch (Exception e) {
                    log.warn("Failed to send BIDI frame on sessionId {}: {}", sessionId, e.getMessage());
                }
            }
        }
    }
}
