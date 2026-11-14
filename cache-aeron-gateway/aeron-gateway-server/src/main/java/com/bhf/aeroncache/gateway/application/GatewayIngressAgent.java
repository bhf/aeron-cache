package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.gateway.codec.GatewayResponseWriter;
import com.bhf.aeroncache.gateway.metrics.GatewayMetrics;
import com.bhf.aeroncache.gateway.metrics.GatewayRequestType;
import com.bhf.aeroncache.gateway.messages.BooleanType;
import com.bhf.aeroncache.gateway.messages.GatewayBulkRequestDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayCommandDecoder;
import com.bhf.aeroncache.gateway.messages.GatewaySubscribeDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayUnsubscribeDecoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.near.NearCache;
import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.AllTimersResult;
import com.bhf.aeroncache.models.results.BulkCacheOpsResult;
import com.bhf.aeroncache.models.results.CacheOperationResultDetails;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CacheStats;
import com.bhf.aeroncache.models.results.CacheStatsResult;
import com.bhf.aeroncache.models.results.TimerDetails;
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
import com.bhf.aeroncache.transport.TransportMedia;
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
    private final NearCacheManager<Object> cacheNearCache;
    private final NearCacheManager<Object> countersNearCache;
    private final GatewayResponseWriter egressWriter;
    private final GatewayMetrics metrics;

    private final ManyToOneConcurrentArrayQueue<Image> unavailableImages = new ManyToOneConcurrentArrayQueue<>(1024);

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final GatewayCommandDecoder commandDecoder = new GatewayCommandDecoder();
    private final GatewaySubscribeDecoder subscribeDecoder = new GatewaySubscribeDecoder();
    private final GatewayUnsubscribeDecoder unsubscribeDecoder = new GatewayUnsubscribeDecoder();
    private final GatewayBulkRequestDecoder bulkRequestDecoder = new GatewayBulkRequestDecoder();
    private final FragmentAssembler fragmentAssembler = new FragmentAssembler(this::onFragment);

    // Reused intermediate structures. The ingress agent thread decodes requests (bulkRequestOps); the
    // cluster egress listener thread builds responses (the remaining flyweight lists and the entries map).
    // The two thread domains never share an instance, and each request/response is encoded synchronously
    // before the structure is rebuilt, so nothing escapes the thread that owns it.
    private final FlyweightList<ReusableCacheOperation> bulkRequestOps =
            new FlyweightList<>(ReusableCacheOperation::new);
    private final GatewayBulkOpsRequest bulkRequest = new GatewayBulkOpsRequest();
    private final FlyweightList<GatewayResponseWriter.BulkOpResultEntry> bulkResultEntries =
            new FlyweightList<>(GatewayResponseWriter.BulkOpResultEntry::new);
    private final FlyweightList<GatewayResponseWriter.StatEntry> statEntries =
            new FlyweightList<>(GatewayResponseWriter.StatEntry::new);
    private final FlyweightList<GatewayResponseWriter.TimerEntry> timerEntries =
            new FlyweightList<>(GatewayResponseWriter.TimerEntry::new);
    private final Map<String, String> streamEntryItems = new HashMap<>();

    // Near-cache reads are served synchronously on the ingress agent thread (not the egress thread), so they
    // must not touch egressWriter / streamEntryItems, whose single buffer is owned by the egress thread. These
    // two structures are the ingress thread's own: a dedicated response writer (with its own encode buffer)
    // and an entries map, both only ever touched here on the ingress thread. The session response Publication
    // is Aeron's concurrent variant, so the ingress and egress threads can each write to it with their own
    // writer safely.
    private final GatewayResponseWriter ingressWriter = new GatewayResponseWriter();
    private final Map<String, String> ingressStreamEntryItems = new HashMap<>();

    private Subscription subscription;

    public GatewayIngressAgent(Aeron aeron,
                               TransportMedia media,
                               String requestEndpoint,
                               String responseControlEndpoint,
                               int requestStreamId,
                               int responseStreamId,
                               AeronCache cluster,
                               GatewaySubscriptionPublisher cacheSubs,
                               GatewaySubscriptionPublisher countersSubs,
                               NearCacheManager<Object> cacheNearCache,
                               NearCacheManager<Object> countersNearCache,
                               GatewayResponseWriter egressWriter,
                               GatewayMetrics metrics) {
        this.aeron = aeron;
        this.requestStreamId = requestStreamId;
        this.cluster = cluster;
        this.cacheSubs = cacheSubs;
        this.countersSubs = countersSubs;
        this.cacheNearCache = cacheNearCache;
        this.countersNearCache = countersNearCache;
        this.egressWriter = egressWriter;
        this.metrics = metrics;
        this.requestUriBuilder = media.requestSubscription(requestEndpoint, responseControlEndpoint);
        this.registry = new GatewaySessionRegistry(aeron, media, responseControlEndpoint, responseStreamId);
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
        metrics.onSessionConnected();
    }

    private void onImageUnavailable(Image image) {
        metrics.onSessionDisconnected();
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
            metrics.onRequest(GatewayRequestType.SUBSCRIBE);
            handleSubscribe(responsePublication, sessionId);
        } else if (templateId == GatewayUnsubscribeDecoder.TEMPLATE_ID) {
            unsubscribeDecoder.wrap(buffer, bodyOffset, blockLength, version);
            metrics.onRequest(GatewayRequestType.UNSUBSCRIBE);
            handleUnsubscribe(sessionId);
        } else if (templateId == GatewayBulkRequestDecoder.TEMPLATE_ID) {
            bulkRequestDecoder.wrap(buffer, bodyOffset, blockLength, version);
            metrics.onRequest(GatewayRequestType.BULK_OPS);
            handleBulkRequest(responsePublication);
        } else {
            metrics.onRequest(GatewayRequestType.UNKNOWN);
            log.warn("Unknown gateway template id {} on session {}", templateId, sessionId);
        }
    }

    // ------------------------------------------------------------------ bulk operations

    /**
     * Decode a bulk request, forward it to the cluster as a single {@link BulkCacheOpsRequest}, and stream
     * the per-operation results back as a bulk response. A bulk request may mix regular-cache and counter
     * operations; the cluster dispatches each to the right cache manager, so we always route the batch
     * through the regular-cache publisher (the cluster egress delivers the single combined result there).
     */
    private void handleBulkRequest(Publication responsePublication) {
        bulkRequestOps.reset();
        for (GatewayBulkRequestDecoder.OperationsDecoder op : bulkRequestDecoder.operations()) {
            bulkRequestOps.next().set(
                    mapBulkOperationType(op.operationType()),
                    op.ttl(),
                    op.counterValue(),
                    op.requestId(),
                    op.cacheId(),
                    op.key(),
                    op.value());
        }
        final String correlationId = requestId(bulkRequestDecoder.correlationId());

        // The operations are encoded onward to the cluster synchronously inside sendBulkOperationsRequest,
        // and the result consumer captures only the correlation id and response publication, so the reused
        // flyweight operations and request never escape this thread and can be rebuilt on the next bulk request.
        bulkRequest.set(correlationId, bulkRequestOps.view());
        cacheSubs.sendBulkOperationsRequest(correlationId, bulkRequest,
                (Consumer<BulkCacheOpsResult>) o -> respondBulk(responsePublication, correlationId, (BulkCacheOpsResult) o));
    }

    /**
     * Forward a batch of bulk operation results to the client. The cluster streams the results in one or
     * more batches (mirroring {@code BulkCacheOpsResult}); this fires once per batch and forwards the
     * batch's results together with the {@code endOfBatch} flag so the client can detect completion.
     */
    private void respondBulk(Publication publication, String correlationId, BulkCacheOpsResult result) {
        bulkResultEntries.reset();
        for (Object o : result.getOperations()) {
            final CacheOperationResultDetails details = (CacheOperationResultDetails) o;
            final String cacheId = details.getCacheId() == null ? null : String.valueOf(details.getCacheId().value());
            final String key = details.getKey() == null ? null : String.valueOf(details.getKey().value());
            final String value = details.getValue() == null ? null : String.valueOf(details.getValue().value());
            bulkResultEntries.next().set(details.getOperationStatus(), details.getRequestId(), cacheId, key, value);
        }
        egressWriter.writeBulkResponse(publication, correlationId, bulkResultEntries.view(), result.isEndOfBatch());
    }

    private static BulkOperationType mapBulkOperationType(com.bhf.aeroncache.gateway.messages.BulkOperationType type) {
        if (type == null) {
            return BulkOperationType.NONE;
        }
        try {
            return BulkOperationType.valueOf(type.name());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown bulk operation type {}", type);
            return BulkOperationType.NONE;
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

        metrics.onRequest(commandRequestType(msgType));

        switch (msgType) {
            case CREATE_CACHE_MSG_ID -> cacheSubs.sendCreateCache(correlationId, cacheId,
                    (Consumer<CreateCacheResult>) o -> respondStatus(responsePublication, correlationId, ((CreateCacheResult) o).getStatus(), cacheId));
            case ADD_CACHE_ENTRY_MSG_ID -> cacheSubs.addCacheEntry(correlationId, cacheId, key, value, ttl,
                    (Consumer<AddCacheEntryResult>) o -> respondStatus(responsePublication, correlationId, ((AddCacheEntryResult) o).getStatus(), cacheId, key, null));
            case PATCH_CACHE_ENTRY_MSG_ID -> cacheSubs.patchValue(correlationId, cacheId, key, value,
                    (Consumer<PatchValueResult>) o -> respondStatus(responsePublication, correlationId, ((PatchValueResult) o).getStatus(), cacheId, key, null));
            case GET_CACHE_ENTRY_MSG_ID -> {
                if (cacheNearCache != null) {
                    nearGetEntry(responsePublication, correlationId, cacheNearCache, cacheSubs, cacheId, key);
                } else {
                    cacheSubs.getCacheEntry(correlationId, cacheId, key,
                            (Consumer<GetCacheEntryResult>) o -> respondEntry(responsePublication, correlationId, (GetCacheEntryResult) o));
                }
            }
            case CLEAR_CACHE_MSG_ID -> cacheSubs.clearCache(correlationId, cacheId,
                    (Consumer<ClearCacheResult>) o -> respondStatus(responsePublication, correlationId, ((ClearCacheResult) o).getStatus(), cacheId));
            case DELETE_CACHE_MSG_ID -> cacheSubs.deleteCache(correlationId, cacheId,
                    (Consumer<DeleteCacheResult>) o -> respondStatus(responsePublication, correlationId, ((DeleteCacheResult) o).getStatus(), cacheId));
            case REMOVE_CACHE_ENTRY_MSG_ID -> cacheSubs.removeCacheEntry(correlationId, cacheId, key,
                    (Consumer<RemoveCacheEntryResult>) o -> respondStatus(responsePublication, correlationId, ((RemoveCacheEntryResult) o).getStatus(), cacheId, key, null));
            case CANCEL_CACHE_ITEM_REMOVAL_MSG_ID -> cacheSubs.cancelItemRemoval(correlationId, cacheId, key,
                    (Consumer<CancelItemRemovalResult>) o -> respondStatus(responsePublication, correlationId, ((CancelItemRemovalResult) o).getStatus(), cacheId, key, null));
            case GET_CACHE_ENTRIES_MSG_ID -> {
                if (cacheNearCache != null) {
                    nearGetEntries(responsePublication, correlationId, cacheNearCache, cacheSubs, cacheId);
                } else {
                    cacheSubs.getCacheEntries(correlationId, cacheId,
                            (Consumer<GetAllCacheEntriesResult>) o -> streamEntries(responsePublication, correlationId, (GetAllCacheEntriesResult) o));
                }
            }
            case GET_CACHE_STATS_MSG_ID -> cacheSubs.getAllCacheStats(correlationId,
                    (Consumer<CacheStatsResult>) o -> streamStats(responsePublication, correlationId, (CacheStatsResult) o));
            case GET_TIMERS_MSG_ID -> cacheSubs.getAllTimers(correlationId,
                    (Consumer<AllTimersResult>) o -> streamTimers(responsePublication, correlationId, (AllTimersResult) o));

            case CREATE_COUNTER_CACHE_MSG_ID -> countersSubs.sendCreateCache(correlationId, cacheId,
                    (Consumer<CreateCacheResult>) o -> respondStatus(responsePublication, correlationId, ((CreateCacheResult) o).getStatus(), cacheId));
            case ADD_COUNTER_ENTRY_MSG_ID -> countersSubs.addCacheEntry(correlationId, cacheId, key, counterValue, ttl,
                    (Consumer<AddCacheEntryResult>) o -> respondStatus(responsePublication, correlationId, ((AddCacheEntryResult) o).getStatus(), cacheId, key, null));
            case GET_COUNTER_ENTRY_MSG_ID -> {
                if (countersNearCache != null) {
                    nearGetEntry(responsePublication, correlationId, countersNearCache, countersSubs, cacheId, key);
                } else {
                    countersSubs.getCacheEntry(correlationId, cacheId, key,
                            (Consumer<GetCacheEntryResult>) o -> respondEntry(responsePublication, correlationId, (GetCacheEntryResult) o));
                }
            }
            case CLEAR_COUNTER_CACHE_MSG_ID -> countersSubs.clearCache(correlationId, cacheId,
                    (Consumer<ClearCacheResult>) o -> respondStatus(responsePublication, correlationId, ((ClearCacheResult) o).getStatus(), cacheId));
            case DELETE_COUNTER_CACHE_MSG_ID -> countersSubs.deleteCache(correlationId, cacheId,
                    (Consumer<DeleteCacheResult>) o -> respondStatus(responsePublication, correlationId, ((DeleteCacheResult) o).getStatus(), cacheId));
            case GET_COUNTER_ENTRIES_MSG_ID -> {
                if (countersNearCache != null) {
                    nearGetEntries(responsePublication, correlationId, countersNearCache, countersSubs, cacheId);
                } else {
                    countersSubs.getCacheEntries(correlationId, cacheId,
                            (Consumer<GetAllCacheEntriesResult>) o -> streamEntries(responsePublication, correlationId, (GetAllCacheEntriesResult) o));
                }
            }
            case GET_COUNTER_STATS_MSG_ID -> countersSubs.getAllCacheStats(correlationId,
                    (Consumer<CacheStatsResult>) o -> streamStats(responsePublication, correlationId, (CacheStatsResult) o));
            case REMOVE_COUNTER_ENTRY_MSG_ID -> countersSubs.removeCacheEntry(correlationId, cacheId, key,
                    (Consumer<RemoveCacheEntryResult>) o -> respondStatus(responsePublication, correlationId, ((RemoveCacheEntryResult) o).getStatus(), cacheId, key, null));
            case CANCEL_COUNTER_ITEM_REMOVAL_MSG_ID -> countersSubs.cancelItemRemoval(correlationId, cacheId, key,
                    (Consumer<CancelItemRemovalResult>) o -> respondStatus(responsePublication, correlationId, ((CancelItemRemovalResult) o).getStatus(), cacheId, key, null));
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

    private static GatewayRequestType commandRequestType(int msgType) {
        return switch (msgType) {
            case CREATE_CACHE_MSG_ID -> GatewayRequestType.CREATE_CACHE;
            case ADD_CACHE_ENTRY_MSG_ID -> GatewayRequestType.ADD_CACHE_ENTRY;
            case PATCH_CACHE_ENTRY_MSG_ID -> GatewayRequestType.PATCH_CACHE_ENTRY;
            case GET_CACHE_ENTRY_MSG_ID -> GatewayRequestType.GET_CACHE_ENTRY;
            case CLEAR_CACHE_MSG_ID -> GatewayRequestType.CLEAR_CACHE;
            case DELETE_CACHE_MSG_ID -> GatewayRequestType.DELETE_CACHE;
            case REMOVE_CACHE_ENTRY_MSG_ID -> GatewayRequestType.REMOVE_CACHE_ENTRY;
            case CANCEL_CACHE_ITEM_REMOVAL_MSG_ID -> GatewayRequestType.CANCEL_CACHE_ITEM_REMOVAL;
            case GET_CACHE_ENTRIES_MSG_ID -> GatewayRequestType.GET_CACHE_ENTRIES;
            case GET_CACHE_STATS_MSG_ID -> GatewayRequestType.GET_CACHE_STATS;
            case GET_TIMERS_MSG_ID -> GatewayRequestType.GET_TIMERS;
            case CREATE_COUNTER_CACHE_MSG_ID -> GatewayRequestType.CREATE_COUNTER_CACHE;
            case ADD_COUNTER_ENTRY_MSG_ID -> GatewayRequestType.ADD_COUNTER_ENTRY;
            case GET_COUNTER_ENTRY_MSG_ID -> GatewayRequestType.GET_COUNTER_ENTRY;
            case CLEAR_COUNTER_CACHE_MSG_ID -> GatewayRequestType.CLEAR_COUNTER_CACHE;
            case DELETE_COUNTER_CACHE_MSG_ID -> GatewayRequestType.DELETE_COUNTER_CACHE;
            case GET_COUNTER_ENTRIES_MSG_ID -> GatewayRequestType.GET_COUNTER_ENTRIES;
            case GET_COUNTER_STATS_MSG_ID -> GatewayRequestType.GET_COUNTER_STATS;
            case REMOVE_COUNTER_ENTRY_MSG_ID -> GatewayRequestType.REMOVE_COUNTER_ENTRY;
            case CANCEL_COUNTER_ITEM_REMOVAL_MSG_ID -> GatewayRequestType.CANCEL_COUNTER_ITEM_REMOVAL;
            case INCREMENT_COUNTER_ENTRY_MSG_ID -> GatewayRequestType.INCREMENT_COUNTER_ENTRY;
            case DECREMENT_COUNTER_ENTRY_MSG_ID -> GatewayRequestType.DECREMENT_COUNTER_ENTRY;
            case SET_COUNTER_ENTRY_MSG_ID -> GatewayRequestType.SET_COUNTER_ENTRY;
            default -> GatewayRequestType.UNKNOWN;
        };
    }

    // ------------------------------------------------------------------ subscriptions

    private void handleSubscribe(Publication responsePublication, String sessionId) {
        final boolean sendSnapshot = subscribeDecoder.sendSnapshot() == BooleanType.T;
        final boolean counters = subscribeDecoder.counters() == BooleanType.T;

        final List<String> cacheIds = new ArrayList<>();
        final List<String> keys = new ArrayList<>();
        com.bhf.aeroncache.models.requests.SubscriptionMode mode = com.bhf.aeroncache.models.requests.SubscriptionMode.FULL;
        final var group = subscribeDecoder.cacheIds();
        while (group.hasNext()) {
            group.next();
            final var entryMode = group.mode();
            cacheIds.add(group.cacheId());
            final var key = group.key();
            keys.add(key == null || key.isEmpty() ? null : key);
            if (entryMode == com.bhf.aeroncache.gateway.messages.SubscriptionMode.PATCH) {
                mode = com.bhf.aeroncache.models.requests.SubscriptionMode.PATCH;
            }
        }
        final String correlationId = requestId(subscribeDecoder.correlationId());

        final GatewaySubscriptionPublisher publisher = counters ? countersSubs : cacheSubs;
        final Consumer<Void> failureHandler = ignored ->
                egressWriter.writeError(responsePublication, correlationId, CacheOperationStatus.ERROR, "Subscription failed");
        final Runnable ackHandler = () ->
                egressWriter.writeSubscribeAck(responsePublication, correlationId, CacheOperationStatus.SUCCESS, cacheIds);
        final Consumer<GatewayStreamUpdate> updateConsumer = update -> {
            egressWriter.writeStreamUpdate(responsePublication, update.eventType(), update.cacheId(),
                    update.key(), update.value(), update.requestId());
            metrics.onStreamUpdatePublished();
        };

        // A subscription where no selector named a key is a whole-cache subscription. Pass a null key list
        // (rather than a list of nulls) so the publisher recognises it as whole-cache and can serve it from
        // the near cache, matching the streaming interfaces' whole-cache semantics.
        boolean anyKey = false;
        for (var key : keys) {
            if (key != null) {
                anyKey = true;
                break;
            }
        }
        final List<String> effectiveKeys = anyKey ? keys : null;

        log.info("Gateway subscribe session {}, caches {}, keys {}, mode {}, snapshot {}, counters {}", sessionId, cacheIds, keys, mode, sendSnapshot, counters);
        publisher.subscribeToCache(cluster, failureHandler, ackHandler, cacheIds, effectiveKeys, mode, sessionId, correlationId, sendSnapshot, updateConsumer);
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
        streamEntryItems.clear();
        result.getValues().forEach((k, v) -> streamEntryItems.put(
                String.valueOf(((Reusable) k).value()),
                v == null ? null : String.valueOf(((Reusable) v).value())));
        egressWriter.writeEntries(publication, correlationId, result.getStatus(), cacheId, streamEntryItems, result.isEndOfBatch());
    }

    /**
     * Forward cache stats to the client. The cluster delivers all stats in a single grouped
     * {@code AllCacheStatsResult} frame, so this is streamed as one end-of-batch stats frame.
     */
    private void streamStats(Publication publication, String correlationId, CacheStatsResult result) {
        statEntries.reset();
        for (Object o : result.getStats()) {
            final CacheStats stat = (CacheStats) o;
            statEntries.next().set(
                    String.valueOf(stat.getCacheId().value()),
                    stat.addedCount, stat.removedCount, stat.clearedCount, stat.size);
        }
        egressWriter.writeStats(publication, correlationId, result.getOperationStatus(), statEntries.view(), true);
    }

    private void streamTimers(Publication publication, String correlationId, AllTimersResult result) {
        timerEntries.reset();
        for (Object o : result.getTimers()) {
            final TimerDetails timer = (TimerDetails) o;
            timerEntries.next().set(
                    timer.timerType.name(),
                    String.valueOf(timer.getCacheId().value()),
                    String.valueOf(timer.getKey().value()),
                    timer.deadline);
        }
        egressWriter.writeTimers(publication, correlationId, result.getOperationStatus(), timerEntries.view(), result.isEndOfBatch());
    }

    private void respondCounter(Publication publication, String correlationId, CacheOperationStatus status,
                                String cacheId, String key, long counterValue) {
        egressWriter.writeCommandResponse(publication, correlationId, status, cacheId, key, Long.toString(counterValue));
    }

    // ------------------------------------------------------------------ near cache reads

    /**
     * Serve a point read from the local near cache, mirroring the HTTP near cache's read semantics: when the
     * cache is warm and the key is present the local value is returned with {@code SUCCESS}; otherwise the
     * cache is primed (idempotently) and {@code UNKNOWN_KEY} is returned. The read never falls through to the
     * cluster. Runs on the ingress thread, so it uses {@link #ingressWriter}, not {@link #egressWriter}.
     */
    private void nearGetEntry(Publication publication, String correlationId, NearCacheManager<Object> near,
                              GatewaySubscriptionPublisher publisher, String cacheId, String key) {
        if (near.contains(cacheId)) {
            final NearCache<String, Object> local = near.get(cacheId);
            if (local.containsKey(key)) {
                final Object value = local.get(key);
                ingressWriter.writeCommandResponse(publication, correlationId, CacheOperationStatus.SUCCESS,
                        cacheId, key, value == null ? null : String.valueOf(value));
                return;
            }
        }
        publisher.primeNearCache(cluster, cacheId);
        ingressWriter.writeCommandResponse(publication, correlationId, CacheOperationStatus.UNKNOWN_KEY, cacheId, key, "");
    }

    /**
     * Serve a whole-cache read from the local near cache, mirroring the HTTP near cache's read semantics:
     * when the cache is warm its local snapshot is written as a single end-of-batch entries frame with
     * {@code SUCCESS}; otherwise the cache is primed (idempotently) and an empty {@code UNKNOWN_CACHE} frame
     * is returned. The read never falls through to the cluster. Runs on the ingress thread, so it uses
     * {@link #ingressWriter} and {@link #ingressStreamEntryItems}.
     */
    private void nearGetEntries(Publication publication, String correlationId, NearCacheManager<Object> near,
                                GatewaySubscriptionPublisher publisher, String cacheId) {
        ingressStreamEntryItems.clear();
        if (near.contains(cacheId)) {
            final NearCache<String, Object> local = near.get(cacheId);
            local.snapshot().forEach((k, v) -> ingressStreamEntryItems.put(k, v == null ? null : String.valueOf(v)));
            ingressWriter.writeEntries(publication, correlationId, CacheOperationStatus.SUCCESS, cacheId, ingressStreamEntryItems, true);
            return;
        }
        publisher.primeNearCache(cluster, cacheId);
        ingressWriter.writeEntries(publication, correlationId, CacheOperationStatus.UNKNOWN_CACHE, cacheId, ingressStreamEntryItems, true);
    }

    private static String requestId(String correlationId) {
        return (correlationId == null || correlationId.isEmpty()) ? UUID.randomUUID().toString() : correlationId;
    }
}
