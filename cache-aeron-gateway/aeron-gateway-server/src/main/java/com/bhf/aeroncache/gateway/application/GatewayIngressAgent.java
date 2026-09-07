package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.gateway.codec.GatewayResponseWriter;
import com.bhf.aeroncache.gateway.messages.BooleanType;
import com.bhf.aeroncache.gateway.messages.GatewayCommandDecoder;
import com.bhf.aeroncache.gateway.messages.GatewaySubscribeDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayUnsubscribeDecoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CacheStats;
import com.bhf.aeroncache.models.results.CacheStatsResult;
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
import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.Header;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.bhf.aeroncache.models.CacheRequestMessageTypes.*;

/**
 * The client facing Aeron endpoint for the gateway.
 * <p>
 * Modeled on {@link io.aeron.response.ResponseServer}: a single request subscription
 * receives command and subscription frames from all clients, and Aeron response channels
 * ({@code control-mode=response}) are used to publish responses/updates back to each client
 * without an application level handshake. Session identity is the request {@link Image}
 * {@code correlationId()}. On image unavailability the session's subscriptions are torn down.
 * <p>
 * Runs on its own {@link org.agrona.concurrent.AgentRunner}. Response frames are written by
 * the cluster egress listener thread via callbacks captured here, each targeting the relevant
 * session's response publication (single writer per publication).
 */
@Log4j2
@SuppressWarnings({"unchecked", "rawtypes"})
public class GatewayIngressAgent implements Agent {

    private static final int FRAGMENT_LIMIT = 10;

    private final Aeron aeron;
    private final int requestStreamId;
    private final ChannelUriStringBuilder requestUriBuilder;
    private final GatewaySessionRegistry registry;

    private final AeronCache cluster;
    private final GatewaySubscriptionPublisher cacheSubs;
    private final GatewaySubscriptionPublisher countersSubs;
    private final GatewayResponseWriter egressWriter;

    private final ManyToOneConcurrentArrayQueue<Image> unavailableImages = new ManyToOneConcurrentArrayQueue<>(1024);

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final GatewayCommandDecoder commandDecoder = new GatewayCommandDecoder();
    private final GatewaySubscribeDecoder subscribeDecoder = new GatewaySubscribeDecoder();
    private final GatewayUnsubscribeDecoder unsubscribeDecoder = new GatewayUnsubscribeDecoder();
    private final FragmentAssembler fragmentAssembler = new FragmentAssembler(this::onFragment);

    private Subscription subscription;

    public GatewayIngressAgent(Aeron aeron,
                               String requestEndpoint,
                               String responseControlEndpoint,
                               int requestStreamId,
                               int responseStreamId,
                               AeronCache cluster,
                               GatewaySubscriptionPublisher cacheSubs,
                               GatewaySubscriptionPublisher countersSubs,
                               GatewayResponseWriter egressWriter) {
        this.aeron = aeron;
        this.requestStreamId = requestStreamId;
        this.cluster = cluster;
        this.cacheSubs = cacheSubs;
        this.countersSubs = countersSubs;
        this.egressWriter = egressWriter;
        this.requestUriBuilder = new ChannelUriStringBuilder()
                .media("udp")
                .endpoint(requestEndpoint)
                .responseEndpoint(responseControlEndpoint);
        this.registry = new GatewaySessionRegistry(aeron, responseControlEndpoint, responseStreamId);
    }

    @Override
    public int doWork() {
        int work = 0;

        if (subscription == null) {
            subscription = aeron.addSubscription(
                    requestUriBuilder.build(),
                    requestStreamId,
                    this::onImageAvailable,
                    this::onImageUnavailable);
            work++;
        }

        Image image;
        while ((image = unavailableImages.poll()) != null) {
            teardownSession(image);
            work++;
        }

        work += subscription.poll(fragmentAssembler, FRAGMENT_LIMIT);
        return work;
    }

    @Override
    public void onClose() {
        CloseHelper.quietClose(subscription);
        registry.close();
    }

    @Override
    public String roleName() {
        return "AeronCache-Gateway-Ingress";
    }

    private void onImageAvailable(Image image) {
        log.info("Gateway client image available, session {}", image.correlationId());
    }

    private void onImageUnavailable(Image image) {
        if (!unavailableImages.offer(image)) {
            log.error("Unable to enqueue unavailable image for session {}", image.correlationId());
        }
    }

    private void teardownSession(Image image) {
        final long correlationId = image.correlationId();
        final String sessionId = Long.toString(correlationId);
        final String requestId = UUID.randomUUID().toString();
        log.info("Tearing down gateway session {}", sessionId);
        cacheSubs.handleClosed(cluster, requestId, sessionId);
        countersSubs.handleClosed(cluster, requestId, sessionId);
        registry.remove(correlationId);
    }

    private void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final Image image = (Image) header.context();
        final Publication responsePublication = registry.getOrCreate(image);
        final String sessionId = Long.toString(image.correlationId());

        headerDecoder.wrap(buffer, offset);
        final int templateId = headerDecoder.templateId();
        final int blockLength = headerDecoder.blockLength();
        final int version = headerDecoder.version();
        final int bodyOffset = offset + headerDecoder.encodedLength();

        if (templateId == GatewayCommandDecoder.TEMPLATE_ID) {
            commandDecoder.wrap(buffer, bodyOffset, blockLength, version);
            handleCommand(responsePublication);
        } else if (templateId == GatewaySubscribeDecoder.TEMPLATE_ID) {
            subscribeDecoder.wrap(buffer, bodyOffset, blockLength, version);
            handleSubscribe(responsePublication, sessionId);
        } else if (templateId == GatewayUnsubscribeDecoder.TEMPLATE_ID) {
            unsubscribeDecoder.wrap(buffer, bodyOffset, blockLength, version);
            handleUnsubscribe(sessionId);
        } else {
            log.warn("Unknown gateway template id {} on session {}", templateId, sessionId);
        }
    }

    // ------------------------------------------------------------------ commands

    private void handleCommand(Publication responsePublication) {
        final int msgType = commandDecoder.msgType();
        final long ttl = commandDecoder.ttl();
        final long counterValue = commandDecoder.counterValue();
        final String correlationId = requestId(commandDecoder.correlationId());
        final String cacheId = commandDecoder.cacheId();
        final String key = commandDecoder.key();
        final String value = commandDecoder.value();

        switch (msgType) {
            case CREATE_CACHE_MSG_ID -> cacheSubs.sendCreateCache(correlationId, cacheId,
                    (Consumer<CreateCacheResult>) o -> respondStatus(responsePublication, correlationId, ((CreateCacheResult) o).getStatus(), cacheId));
            case ADD_CACHE_ENTRY_MSG_ID -> cacheSubs.addCacheEntry(correlationId, cacheId, key, value, ttl,
                    (Consumer<AddCacheEntryResult>) o -> respondStatus(responsePublication, correlationId, ((AddCacheEntryResult) o).getStatus(), cacheId, key, null));
            case PATCH_CACHE_ENTRY_MSG_ID -> cacheSubs.patchValue(correlationId, cacheId, key, value,
                    (Consumer<PatchValueResult>) o -> respondStatus(responsePublication, correlationId, ((PatchValueResult) o).getStatus(), cacheId, key, null));
            case GET_CACHE_ENTRY_MSG_ID -> cacheSubs.getCacheEntry(correlationId, cacheId, key,
                    (Consumer<GetCacheEntryResult>) o -> respondEntry(responsePublication, correlationId, (GetCacheEntryResult) o));
            case CLEAR_CACHE_MSG_ID -> cacheSubs.clearCache(correlationId, cacheId,
                    (Consumer<ClearCacheResult>) o -> respondStatus(responsePublication, correlationId, ((ClearCacheResult) o).getStatus(), cacheId));
            case DELETE_CACHE_MSG_ID -> cacheSubs.deleteCache(correlationId, cacheId,
                    (Consumer<DeleteCacheResult>) o -> respondStatus(responsePublication, correlationId, ((DeleteCacheResult) o).getStatus(), cacheId));
            case REMOVE_CACHE_ENTRY_MSG_ID -> cacheSubs.removeCacheEntry(correlationId, cacheId, key,
                    (Consumer<RemoveCacheEntryResult>) o -> respondStatus(responsePublication, correlationId, ((RemoveCacheEntryResult) o).getStatus(), cacheId, key, null));
            case GET_CACHE_ENTRIES_MSG_ID -> cacheSubs.getCacheEntries(correlationId, cacheId,
                    (Consumer<GetAllCacheEntriesResult>) o -> streamEntries(responsePublication, correlationId, (GetAllCacheEntriesResult) o));
            case GET_CACHE_STATS_MSG_ID -> cacheSubs.getAllCacheStats(correlationId,
                    (Consumer<CacheStatsResult>) o -> streamStats(responsePublication, correlationId, (CacheStatsResult) o));

            case CREATE_COUNTER_CACHE_MSG_ID -> countersSubs.sendCreateCache(correlationId, cacheId,
                    (Consumer<CreateCacheResult>) o -> respondStatus(responsePublication, correlationId, ((CreateCacheResult) o).getStatus(), cacheId));
            case ADD_COUNTER_ENTRY_MSG_ID -> countersSubs.addCacheEntry(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<AddCacheEntryResult>) o -> respondStatus(responsePublication, correlationId, ((AddCacheEntryResult) o).getStatus(), cacheId, key, null));
            case GET_COUNTER_ENTRY_MSG_ID -> countersSubs.getCacheEntry(correlationId, cacheId, key,
                    (Consumer<GetCacheEntryResult>) o -> respondEntry(responsePublication, correlationId, (GetCacheEntryResult) o));
            case CLEAR_COUNTER_CACHE_MSG_ID -> countersSubs.clearCache(correlationId, cacheId,
                    (Consumer<ClearCacheResult>) o -> respondStatus(responsePublication, correlationId, ((ClearCacheResult) o).getStatus(), cacheId));
            case DELETE_COUNTER_CACHE_MSG_ID -> countersSubs.deleteCache(correlationId, cacheId,
                    (Consumer<DeleteCacheResult>) o -> respondStatus(responsePublication, correlationId, ((DeleteCacheResult) o).getStatus(), cacheId));
            case GET_COUNTER_ENTRIES_MSG_ID -> countersSubs.getCacheEntries(correlationId, cacheId,
                    (Consumer<GetAllCacheEntriesResult>) o -> streamEntries(responsePublication, correlationId, (GetAllCacheEntriesResult) o));
            case GET_COUNTER_STATS_MSG_ID -> countersSubs.getAllCacheStats(correlationId,
                    (Consumer<CacheStatsResult>) o -> streamStats(responsePublication, correlationId, (CacheStatsResult) o));
            case REMOVE_COUNTER_ENTRY_MSG_ID -> countersSubs.removeCacheEntry(correlationId, cacheId, key,
                    (Consumer<RemoveCacheEntryResult>) o -> respondStatus(responsePublication, correlationId, ((RemoveCacheEntryResult) o).getStatus(), cacheId, key, null));
            case INCREMENT_COUNTER_ENTRY_MSG_ID -> countersSubs.incrementCounter(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<IncrementCounterResult>) o -> respondCounter(responsePublication, correlationId, ((IncrementCounterResult) o).getStatus(), cacheId, key, ((IncrementCounterResult) o).getCounterValue()));
            case DECREMENT_COUNTER_ENTRY_MSG_ID -> countersSubs.decrementCounter(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<DecrementCounterResult>) o -> respondCounter(responsePublication, correlationId, ((DecrementCounterResult) o).getStatus(), cacheId, key, ((DecrementCounterResult) o).getCounterValue()));
            case SET_COUNTER_ENTRY_MSG_ID -> countersSubs.setCounter(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<SetCounterResult>) o -> respondCounter(responsePublication, correlationId, ((SetCounterResult) o).getStatus(), cacheId, key, ((SetCounterResult) o).getCounterValue()));

            default -> {
                log.warn("Unknown gateway command msgType {}", msgType);
                egressWriter.writeError(responsePublication, correlationId, CacheOperationStatus.ERROR, "Unknown msgType " + msgType);
            }
        }
    }

    // ------------------------------------------------------------------ subscriptions

    private void handleSubscribe(Publication responsePublication, String sessionId) {
        final boolean sendSnapshot = subscribeDecoder.sendSnapshot() == BooleanType.T;
        final boolean counters = subscribeDecoder.counters() == BooleanType.T;

        final List<String> cacheIds = new ArrayList<>();
        final var group = subscribeDecoder.cacheIds();
        while (group.hasNext()) {
            group.next();
            cacheIds.add(group.cacheId());
        }
        final String correlationId = requestId(subscribeDecoder.correlationId());

        final GatewaySubscriptionPublisher publisher = counters ? countersSubs : cacheSubs;
        final Consumer<Void> failureHandler = ignored ->
                egressWriter.writeError(responsePublication, correlationId, CacheOperationStatus.ERROR, "Subscription failed");
        final Consumer<CacheUpdateEvent> updateConsumer = event ->
                egressWriter.writeStreamUpdate(responsePublication, event);

        log.info("Gateway subscribe session {}, caches {}, snapshot {}, counters {}", sessionId, cacheIds, sendSnapshot, counters);
        publisher.subscribeToCache(cluster, failureHandler, cacheIds, sessionId, correlationId, sendSnapshot, updateConsumer);
    }

    private void handleUnsubscribe(String sessionId) {
        final boolean counters = unsubscribeDecoder.counters() == BooleanType.T;
        final String correlationId = requestId(unsubscribeDecoder.correlationId());
        final String cacheId = unsubscribeDecoder.cacheId();

        final GatewaySubscriptionPublisher publisher = counters ? countersSubs : cacheSubs;
        log.info("Gateway unsubscribe session {}, cache {}, counters {}", sessionId, cacheId, counters);
        publisher.unsubscribeSession(cluster, correlationId, sessionId, cacheId);
    }

    // ------------------------------------------------------------------ response helpers

    private void respondStatus(Publication publication, String correlationId, CacheOperationStatus status, String cacheId) {
        respondStatus(publication, correlationId, status, cacheId, null, null);
    }

    private void respondStatus(Publication publication, String correlationId, CacheOperationStatus status,
                               String cacheId, String key, String value) {
        egressWriter.writeCommandResponse(publication, correlationId, status, cacheId, key, value);
    }

    private void respondEntry(Publication publication, String correlationId, GetCacheEntryResult result) {
        final String cacheId = String.valueOf(result.getCacheId().value());
        final String key = result.getEntryKey() == null ? null : String.valueOf(result.getEntryKey().value());
        final String value = result.getEntryValue() == null ? null : String.valueOf(result.getEntryValue().value());
        egressWriter.writeCommandResponse(publication, correlationId, result.getStatus(), cacheId, key, value);
    }

    /**
     * Forward a batch of cache/counter entries to the client. The cluster streams entries in one or more
     * batches (mirroring {@code AllCacheEntriesResult}); this fires once per batch and forwards the
     * batch's items together with the {@code endOfBatch} flag so the client can detect completion.
     */
    private void streamEntries(Publication publication, String correlationId, GetAllCacheEntriesResult result) {
        final String cacheId = String.valueOf(result.getCacheId().value());
        final Map<String, String> items = new HashMap<>();
        result.getValues().forEach((k, v) -> items.put(
                String.valueOf(((Reusable) k).value()),
                v == null ? null : String.valueOf(((Reusable) v).value())));
        egressWriter.writeEntries(publication, correlationId, result.getStatus(), cacheId, items, result.isEndOfBatch());
    }

    /**
     * Forward cache stats to the client. The cluster delivers all stats in a single grouped
     * {@code AllCacheStatsResult} frame, so this is streamed as one end-of-batch stats frame.
     */
    private void streamStats(Publication publication, String correlationId, CacheStatsResult result) {
        final List<GatewayResponseWriter.StatEntry> stats = new ArrayList<>();
        for (Object o : result.getStats()) {
            final CacheStats stat = (CacheStats) o;
            stats.add(new GatewayResponseWriter.StatEntry(
                    String.valueOf(stat.getCacheId().value()),
                    stat.addedCount, stat.removedCount, stat.clearedCount, stat.size));
        }
        egressWriter.writeStats(publication, correlationId, result.getOperationStatus(), stats, true);
    }

    private void respondCounter(Publication publication, String correlationId, CacheOperationStatus status,
                                String cacheId, String key, long counterValue) {
        egressWriter.writeCommandResponse(publication, correlationId, status, cacheId, key, Long.toString(counterValue));
    }

    private static String requestId(String correlationId) {
        return (correlationId == null || correlationId.isEmpty()) ? UUID.randomUUID().toString() : correlationId;
    }
}
