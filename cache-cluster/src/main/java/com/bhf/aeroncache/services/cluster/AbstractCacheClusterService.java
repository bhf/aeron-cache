package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CacheRequestDecoder;
import com.bhf.aeroncache.codecs.request.CountersCacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.CacheResponseEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseEncoder;
import com.bhf.aeroncache.handlers.NoOpPublicationFailureHandler;
import com.bhf.aeroncache.handlers.PublicationFailureHandler;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.consumer.HydratingPublicationConsumer;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.CacheTimerService;
import com.bhf.aeroncache.services.cache.Cache;
import com.bhf.aeroncache.services.cachemanager.CacheManager;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.cachemanager.CacheSchemaDetailsProvider;
import com.bhf.aeroncache.services.cachemanager.CountersCacheManager;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionServiceImpl;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The cache cluster service provides access to a CacheManager via an
 * Aeron cluster interface. It processes the core messages of the cache and
 * delegates those to the implementation of the
 * {@link CacheManager}.
 */
@Log4j2
public class AbstractCacheClusterService<I extends Reusable, K extends Reusable, V extends Reusable> implements ClusteredService {

    private String nodeId;
    private final Supplier<I> indexSupplier;
    private final CacheSchemaDetailsProvider schemaDetails;
    private Cluster cluster;
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();
    private final CacheTracingService tracingService;
    private final PublicationFailureHandler publicationFailureHandler = new NoOpPublicationFailureHandler();
    private final TimerCorrelationIdProvider timerCorrelationIdProvider = new TimerCorrelationIdProvider();
    private IdleStrategy idleStrategy;

    final CreateCacheRequestDetails<I> createCacheRequestDetails;
    final ClearCacheRequestDetails<I> clearCacheRequestDetails;
    final RemoveCacheEntryRequestDetails<I, K> removeCacheEntryRequestDetails;
    final CancelItemRemovalRequestDetails<I, K> cancelItemRemovalRequestDetails;
    final CancelItemRemovalResult<I, K> cancelItemRemovalResult;
    final PatchValueRequestDetails<I, K, V> patchValueRequestDetails;
    final AddCacheEntryRequestDetails<I, K, V> addCacheEntryRequestDetails;
    final DeleteCacheRequestDetails<I> deleteCacheRequestDetails;
    final GetCacheEntryRequestDetails<I, K> getCacheEntryRequestDetails;
    final GetAllCacheEntriesRequestDetails<I> getAllCacheEntriesRequestDetails;
    final AddCacheEntryResult<I, K> addEntryFailureResult;
    final AddCacheEntryResult<I, K> patchEntryUpdateResult;
    final GetCacheStatsRequestDetails getCacheStatsRequestDetails;
    final CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails;
    final CacheUnsubscribeRequestDetails<I> cacheUnsubscribeRequestDetails;
    final BulkCacheOpsRequestDetails<I,K,V> bulkCacheOpsRequestDetails;
    final BulkCacheOpsResult<I, K, V> bulkOpsResult;
    final CacheSubscriptionResult<I,K,V> subscribeResult;
    final CacheUnsubscribeResult<I> unsubscribeResult;

    private final CacheManagerFactory<I, K, V> cacheManagerFactory;

    private final CacheManager<I, K, V> cacheManager;
    protected CacheSubscriptionService<I, K, V> subscriptionService;
    private final CacheRequestDecoder<I, K, V> decoder;
    private final CacheResponseEncoder<I, K, V> encoder;
    private final Comparator<K> keyComparator;

    private final CountersCacheManager<I, K, ReusableLong> countersCacheManager;
    protected CacheSubscriptionService<I, K, ReusableLong> countersSubscriptionService;
    private final CountersCacheRequestDecoder<I, K, ReusableLong> countersRequestDecoder;
    private final CountersCacheResponseEncoder<I, K, ReusableLong> countersResponseEncoder;
    private final AddCacheEntryRequestDetails<I, K, ReusableLong> addCountersCacheEntryRequestDetails;
    private final CacheSubscriptionResult<I,K, ReusableLong> countersSubscribeResult;
    private final Supplier<ReusableLong> counterCacheValueSupplier = ReusableLong::new;
    private final IncrementCounterRequestDetails<I, K> incrementCounterRequestDetails;
    private final DecrementCounterRequestDetails<I, K> decrementCounterRequestDetails;
    private final SetCounterRequestDetails<I, K> setCounterRequestDetails;
    private final IncrementCounterResult<I, K> incrementCounterResult;
    private final DecrementCounterResult<I, K> decrementCounterResult;
    private final SetCounterResult<I, K> setCounterResult;

    @Setter
    @Getter
    private boolean dynamicCacheCreationEnabled = false;
    private CacheTimerService<I, K> cacheTimerService;
    private CacheTimerService<I, K> cacheCountersTimerService;


    protected AbstractCacheClusterService(String nodeId, CacheTracingService tracingService, CacheManagerFactory<I, K, V> cacheManagerFactory) {
        this.createCacheRequestDetails = new CreateCacheRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.clearCacheRequestDetails = new ClearCacheRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.removeCacheEntryRequestDetails = new RemoveCacheEntryRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.cancelItemRemovalRequestDetails = new CancelItemRemovalRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.cancelItemRemovalResult = new CancelItemRemovalResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.patchValueRequestDetails = new PatchValueRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get(), cacheManagerFactory.getValueSupplier().get());
        this.addCacheEntryRequestDetails = new AddCacheEntryRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get(), cacheManagerFactory.getValueSupplier().get());
        this.deleteCacheRequestDetails = new DeleteCacheRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.getCacheEntryRequestDetails = new GetCacheEntryRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.getAllCacheEntriesRequestDetails = new GetAllCacheEntriesRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.addEntryFailureResult = new AddCacheEntryResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.patchEntryUpdateResult = new AddCacheEntryResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.getCacheStatsRequestDetails = new GetCacheStatsRequestDetails();
        this.cacheSubscribeRequestDetails = new CacheSubscriptionRequestDetails<>();
        this.cacheUnsubscribeRequestDetails = new CacheUnsubscribeRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.bulkCacheOpsRequestDetails = new BulkCacheOpsRequestDetails<>(cacheManagerFactory.getIndexSupplier(), cacheManagerFactory.getKeySupplier(), cacheManagerFactory.getValueSupplier());
        this.subscribeResult = new CacheSubscriptionResult<>(cacheManagerFactory.getIndexSupplier().get());
        this.unsubscribeResult = new CacheUnsubscribeResult<>(cacheManagerFactory.getIndexSupplier().get());
        this.cacheManagerFactory = cacheManagerFactory;
        this.cacheManager = cacheManagerFactory.getCacheManager();
        this.countersCacheManager = cacheManagerFactory.getCountersCacheManager();
        this.nodeId = nodeId;
        this.tracingService = tracingService;
        this.indexSupplier = cacheManagerFactory.getIndexSupplier();
        this.schemaDetails = cacheManagerFactory.getSchemaDetailsProvider();
        this.bulkOpsResult = new BulkCacheOpsResult<>(cacheManagerFactory.getIndexSupplier(),
                cacheManagerFactory.getKeySupplier(), cacheManagerFactory.getValueSupplier());

        this.decoder = cacheManagerFactory.getCacheRequestDecoder();
        this.encoder = cacheManagerFactory.getCacheResponseEncoder();
        this.keyComparator = cacheManagerFactory.getKeyComparator();

        this.countersRequestDecoder = cacheManagerFactory.getCountersRequestDecoder();
        this.countersResponseEncoder = cacheManagerFactory.getCountersResponseEncoder();
        this.addCountersCacheEntryRequestDetails = new AddCacheEntryRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get(), new ReusableLong());
        this.countersSubscribeResult = new CacheSubscriptionResult<>(cacheManagerFactory.getIndexSupplier().get());

        this.incrementCounterRequestDetails = new IncrementCounterRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.decrementCounterRequestDetails = new DecrementCounterRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.setCounterRequestDetails = new SetCounterRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.incrementCounterResult = new IncrementCounterResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.decrementCounterResult = new DecrementCounterResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.setCounterResult = new SetCounterResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
    }

    /**
     * Process messages from cache clients.
     *
     * @param session   for the client which sent the message. This can be null if the client was a service.
     * @param timestamp for when the message was received.
     * @param buffer    containing the message.
     * @param offset    in the buffer at which the message is encoded.
     * @param length    of the encoded message.
     * @param header    aeron header for the incoming message.
     */
    public void onSessionMessage(final ClientSession session, final long timestamp, final DirectBuffer buffer, final int offset, final int length, final Header header) {

        final int templateId = (buffer.getShort(offset + 2, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);

        if (templateId == schemaDetails.getCreateCacheId()) {
            handleCreateCache(session, buffer, offset, decoder, encoder, createCacheRequestDetails, cacheManager);
        } else if (templateId == schemaDetails.getAddCacheEntryId()) {
            handleAddCacheEntry(session, buffer, offset, decoder, encoder, addCacheEntryRequestDetails, cacheManager, subscriptionService, cacheTimerService);
        } else if (templateId == schemaDetails.getGetCacheEntryId()) {
            handleGetCacheEntry(session, buffer, offset, decoder, encoder, cacheManager);
        } else if (templateId == schemaDetails.getRemoveCacheEntryId()) {
            handleRemoveCacheEntry(session, buffer, offset, decoder, encoder, cacheManager, subscriptionService);
        } else if (templateId == schemaDetails.getCancelCacheItemRemovalId()) {
            handleCancelItemRemoval(session, buffer, offset, decoder, encoder, cacheTimerService);
        } else if (templateId == schemaDetails.getPatchCacheEntryId()) {
            handlePatchValue(session, buffer, offset, decoder, encoder, patchValueRequestDetails, cacheManager, subscriptionService);
        } else if (templateId == schemaDetails.getClearCacheId()) {
            handleClearCache(session, buffer, offset, decoder, encoder, cacheManager, subscriptionService);
        } else if (templateId == schemaDetails.getDeleteCacheId()) {
            handleDeleteCache(session, buffer, offset, decoder, encoder, cacheManager, subscriptionService);
        } else if (templateId == schemaDetails.getGetAllCacheEntriesId()) {
            handleGetAllCacheEntries(session, buffer, offset, decoder, encoder, cacheManager);
        } else if (templateId == schemaDetails.getGetCacheStatsId()) {
            handleGetCacheStats(session, buffer, offset, decoder, encoder, cacheManager);
        } else if (templateId == schemaDetails.getCacheSubscriptionRequestId()) {
            handleCacheSubscriptionRequest(session, buffer, offset, decoder, encoder, subscriptionService, cacheManager);
        } else if (templateId == schemaDetails.getCacheUnsubscribeRequestId()) {
            handleCacheUnsubscribeRequest(session, buffer, offset, decoder, encoder, subscriptionService, cacheManager);
        } else if (templateId == schemaDetails.getBulkCacheOpsRequestId()) {
            handleBulkOpsRequest(session, buffer, offset, cacheManager, countersCacheManager, encoder, countersResponseEncoder, subscriptionService, countersSubscriptionService,
                    cacheManagerFactory.getValueSupplier(), counterCacheValueSupplier, cacheTimerService, cacheCountersTimerService);
        }
        // Handle Cache Counter messages
        else if (templateId == schemaDetails.getCreateCounterCacheId()) {
            handleCreateCache(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, createCacheRequestDetails, countersCacheManager);
        } else if (templateId == schemaDetails.getAddCounterCacheEntryId()) {
            handleAddCacheEntry(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, addCountersCacheEntryRequestDetails, countersCacheManager, countersSubscriptionService, cacheCountersTimerService);
        } else if (templateId == schemaDetails.getGetCounterCacheEntryId()) {
            handleGetCacheEntry(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersCacheManager);
        } else if (templateId == schemaDetails.getRemoveCounterCacheEntryId()) {
            handleRemoveCacheEntry(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersCacheManager, countersSubscriptionService);
        } else if (templateId == schemaDetails.getCancelCounterItemRemovalId()) {
            handleCancelItemRemoval(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, cacheCountersTimerService);
        } else if (templateId == schemaDetails.getClearCounterCacheId()) {
            handleClearCache(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersCacheManager, countersSubscriptionService);
        } else if (templateId == schemaDetails.getDeleteCounterCacheId()) {
            handleDeleteCache(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersCacheManager, countersSubscriptionService);
        } else if (templateId == schemaDetails.getGetAllCounterCacheEntriesId()) {
            handleGetAllCacheEntries(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersCacheManager);
        } else if (templateId == schemaDetails.getCounterCacheSubscriptionRequestId()) {
            handleCacheSubscriptionRequest(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersSubscriptionService, countersCacheManager);
        } else if (templateId == schemaDetails.getCounterCacheUnsubscribeRequestId()) {
            handleCacheUnsubscribeRequest(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersSubscriptionService, countersCacheManager);
        } else if (templateId == schemaDetails.getCounterIncrementRequestId()) {
            handleIncrementCounterRequest(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersSubscriptionService, countersCacheManager);
        } else if (templateId == schemaDetails.getCounterDecrementRequestId()) {
            handleDecrementCounterRequest(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersSubscriptionService, countersCacheManager);
        } else if (templateId == schemaDetails.getSetCounterRequestId()) {
            handleSetCounterRequest(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersSubscriptionService, countersCacheManager);
        } else if (templateId == schemaDetails.getGetCounterStatsId()) {
            handleGetCacheStats(session, buffer, offset, countersRequestDecoder, countersResponseEncoder, countersCacheManager);
        }
        else {
            log.warn("Unexpected message with ID: {}", templateId);
        }
    }

    /**
     * Cluster started.
     *
     * @param cluster       with which the service can interact.
     * @param snapshotImage from which the service can load its archived state which can be null when no snapshot.
     */
    @Override
    public void onStart(final Cluster cluster, final Image snapshotImage) {
        log.info("On start called on cluster service");
        this.cluster = cluster;

        TimerDetailsFlyweight<I, K> cacheTimersFlyweight = new TimerDetailsFlyweight<>();
        Consumer<TimerDetailsFlyweight<I, K>> cacheTimersConsumer = getCachesTimerDetailsFlyweightConsumer();
        this.cacheTimerService = new CacheClusterTimerService<>(cacheManagerFactory.getIndexSupplier(),
                cacheManagerFactory.getKeySupplier(), cacheManagerFactory.getCacheTimersCodec(), timerCorrelationIdProvider, cluster, cacheTimersFlyweight, cacheTimersConsumer);

        TimerDetailsFlyweight<I, K> countersTimersFlyweight = new TimerDetailsFlyweight<>();
        Consumer<TimerDetailsFlyweight<I, K>> countersTimersConsumer = getCountersTimerDetailsFlyweightConsumer();
        this.cacheCountersTimerService = new CacheClusterTimerService<>(cacheManagerFactory.getIndexSupplier(),
                cacheManagerFactory.getKeySupplier(), cacheManagerFactory.getCacheTimersCodec(), timerCorrelationIdProvider, cluster, countersTimersFlyweight, countersTimersConsumer);

        this.idleStrategy = cluster.idleStrategy();
        if (null != snapshotImage) {
            loadSnapshot(cluster, snapshotImage);
        }

        log.info("Starting subscription service");
        this.subscriptionService = new CacheSubscriptionServiceImpl<>(idleStrategy, subscribeResult, unsubscribeResult, indexSupplier);
        this.countersSubscriptionService = new CacheSubscriptionServiceImpl<>(idleStrategy, countersSubscribeResult, unsubscribeResult, indexSupplier);
    }

    private Consumer<TimerDetailsFlyweight<I, K>> getCountersTimerDetailsFlyweightConsumer() {
        Consumer<TimerDetailsFlyweight<I, K>> consumer = timerDetailsFlyweight -> {
            var cache = timerDetailsFlyweight.getCache();
            var key = timerDetailsFlyweight.getKey();
            var correlationId = timerDetailsFlyweight.getCorrelationId();

            var removeResult = processRemoveCacheEntry(cache, key, "timer-" + correlationId, countersCacheManager);

            if (removeResult.getStatus() == CacheOperationStatus.SUCCESS) {
                // update the subscription service
                handlePostRemoveTimerCacheEntry(cache, key, removeResult, countersResponseEncoder, countersSubscriptionService);
            }
        };
        return consumer;
    }

    private Consumer<TimerDetailsFlyweight<I, K>> getCachesTimerDetailsFlyweightConsumer() {
        Consumer<TimerDetailsFlyweight<I, K>> consumer = timerDetailsFlyweight -> {
            var cache = timerDetailsFlyweight.getCache();
            var key = timerDetailsFlyweight.getKey();
            var correlationId = timerDetailsFlyweight.getCorrelationId();

            var removeResult = processRemoveCacheEntry(cache, key, "timer-" + correlationId, cacheManager);

            if (removeResult.getStatus() == CacheOperationStatus.SUCCESS) {
                // update the subscription service
                handlePostRemoveTimerCacheEntry(cache, key, removeResult, encoder, subscriptionService);
            }
        };
        return consumer;
    }

    /**
     * Handle setting a counter's value.
     *
     * @param session  Session requesting the delete operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param countersRequestDecoder Counters request decoder.
     * @param countersResponseEncoder Counters response decoder.
     * @param countersSubscriptionService Counters subscription service.
     * @param countersCacheManager Counters cache manager.
     */
    private void handleSetCounterRequest(ClientSession session, DirectBuffer buffer, int offset,
                                         CountersCacheRequestDecoder<I, K, ReusableLong> countersRequestDecoder,
                                         CountersCacheResponseEncoder<I, K, ReusableLong> countersResponseEncoder,
                                         CacheSubscriptionService<I, K, ReusableLong> countersSubscriptionService,
                                         CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        countersRequestDecoder.decodeSetCounterRequest(buffer, offset, setCounterRequestDetails);
        I cacheId = setCounterRequestDetails.getCacheId();
        K counterId = setCounterRequestDetails.getCounterId();
        long counterValue = setCounterRequestDetails.getCounterValue();
        var requestId = setCounterRequestDetails.getRequestId();
        var result = processSetCounter(cacheId, counterId, counterValue, requestId, countersCacheManager);
        log.info("Result for set counter, cacheId: {}, counterId: {}, status: {}, counterValue: {}", cacheId, counterId, result.getStatus(), result.getCounterValue());
        handlePostSetCounter(cacheId, result, session, countersResponseEncoder, countersSubscriptionService);
    }

    private SetCounterResult<I, K> processSetCounter(I cacheId, K counterId, long counterValue, String requestId,
                                                      CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        log.info("Got set counter request for cache id {}, counterId {}, counterValue {}, request Id: {}", cacheId, counterId, counterValue, requestId);
        var counterOpResult = countersCacheManager.setCounter(cacheId, counterId, counterValue);
        setCounterResult.clear();
        setCounterResult.getCacheId().copyFrom(counterOpResult.getCacheId());
        setCounterResult.getKey().copyFrom(counterOpResult.getKey());
        setCounterResult.setRequestId(requestId);
        setCounterResult.setStatus(counterOpResult.getStatus());
        setCounterResult.setCounterValue(counterOpResult.getCounterValue());
        return setCounterResult;
    }

    protected void handlePostSetCounter(I cacheId, SetCounterResult<I, K> result, ClientSession session,
                                        CountersCacheResponseEncoder<I, K, ReusableLong> countersResponseEncoder,
                                        CacheSubscriptionService<I, K, ReusableLong> subscriptionService) {
        var length = countersResponseEncoder.encodeSetCounterResult(cacheId, result, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleCounterUpdated(cacheId, egressBuffer, length);
    }

    /**
     * Handle decrementing a counter's value by a specified amount.
     *
     * @param session  Session requesting the delete operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param countersRequestDecoder Counters request decoder.
     * @param countersResponseEncoder Counters response decoder.
     * @param countersSubscriptionService Counters subscription service.
     * @param countersCacheManager Counters cache manager.
     */
    private void handleDecrementCounterRequest(ClientSession session, DirectBuffer buffer, int offset,
                                               CountersCacheRequestDecoder<I, K, ReusableLong> countersRequestDecoder,
                                               CountersCacheResponseEncoder<I, K, ReusableLong> countersResponseEncoder,
                                               CacheSubscriptionService<I, K, ReusableLong> countersSubscriptionService,
                                               CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        countersRequestDecoder.decodeDecrementCounterRequest(buffer, offset, decrementCounterRequestDetails);
        I cacheId = decrementCounterRequestDetails.getCacheId();
        K counterId = decrementCounterRequestDetails.getCounterId();
        long amount = decrementCounterRequestDetails.getAmount();
        var requestId = decrementCounterRequestDetails.getRequestId();
        var result = processDecrementCounter(cacheId, counterId, amount, requestId, countersCacheManager);
        log.info("Result for decrement counter, cacheId: {}, counterId: {}, status: {}, counterValue: {}", cacheId, counterId, result.getStatus(), result.getCounterValue());
        handlePostDecrementCounter(cacheId, result, session, countersResponseEncoder, countersSubscriptionService);
    }

    private DecrementCounterResult<I, K> processDecrementCounter(I cacheId, K counterId, long amount, String requestId,
                                                                  CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        log.info("Got decrement counter request for cache id {}, counterId {}, amount {}, request Id: {}", cacheId, counterId, amount, requestId);
        var counterOpResult = countersCacheManager.decrementCounter(cacheId, counterId, amount);
        decrementCounterResult.clear();
        decrementCounterResult.getCacheId().copyFrom(counterOpResult.getCacheId());
        decrementCounterResult.getKey().copyFrom(counterOpResult.getKey());
        decrementCounterResult.setRequestId(requestId);
        decrementCounterResult.setStatus(counterOpResult.getStatus());
        decrementCounterResult.setCounterValue(counterOpResult.getCounterValue());
        return decrementCounterResult;
    }

    protected void handlePostDecrementCounter(I cacheId, DecrementCounterResult<I, K> result, ClientSession session,
                                              CountersCacheResponseEncoder<I, K, ReusableLong> countersResponseEncoder,
                                              CacheSubscriptionService<I, K, ReusableLong> subscriptionService) {
        var length = countersResponseEncoder.encodeDecrementResult(cacheId, result, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleCounterUpdated(cacheId, egressBuffer, length);
    }

    /**
     * Handle incrementing a counter's value by a specified amount.
     *
     * @param session  Session requesting the delete operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param countersRequestDecoder Counters request decoder.
     * @param countersResponseEncoder Counters response decoder.
     * @param countersSubscriptionService Counters subscription service.
     * @param countersCacheManager Counters cache manager.
     */
    private void handleIncrementCounterRequest(ClientSession session, DirectBuffer buffer, int offset,
                                               CountersCacheRequestDecoder<I, K, ReusableLong> countersRequestDecoder,
                                               CountersCacheResponseEncoder<I, K, ReusableLong> countersResponseEncoder,
                                               CacheSubscriptionService<I, K, ReusableLong> countersSubscriptionService,
                                               CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        countersRequestDecoder.decodeIncrementCounterRequest(buffer, offset, incrementCounterRequestDetails);
        I cacheId = incrementCounterRequestDetails.getCacheId();
        K counterId = incrementCounterRequestDetails.getCounterId();
        long amount = incrementCounterRequestDetails.getAmount();
        var requestId = incrementCounterRequestDetails.getRequestId();
        var result = processIncrementCounter(cacheId, counterId, amount, requestId, countersCacheManager);
        log.info("Result for increment counter, cacheId: {}, counterId: {}, status: {}, counterValue: {}", cacheId, counterId, result.getStatus(), result.getCounterValue());
        handlePostIncrementCounter(cacheId, result, session, countersResponseEncoder, countersSubscriptionService);
    }

    private IncrementCounterResult<I, K> processIncrementCounter(I cacheId, K counterId, long amount, String requestId,
                                                                  CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        log.info("Got increment counter request for cache id {}, counterId {}, amount {}, request Id: {}", cacheId, counterId, amount, requestId);
        var counterOpResult = countersCacheManager.incrementCounter(cacheId, counterId, amount);
        incrementCounterResult.clear();
        incrementCounterResult.getCacheId().copyFrom(counterOpResult.getCacheId());
        incrementCounterResult.getKey().copyFrom(counterOpResult.getKey());
        incrementCounterResult.setRequestId(requestId);
        incrementCounterResult.setStatus(counterOpResult.getStatus());
        incrementCounterResult.setCounterValue(counterOpResult.getCounterValue());
        return incrementCounterResult;
    }

    protected void handlePostIncrementCounter(I cacheId, IncrementCounterResult<I, K> result, ClientSession session,
                                              CountersCacheResponseEncoder<I, K, ReusableLong> countersResponseEncoder,
                                              CacheSubscriptionService<I, K, ReusableLong> subscriptionService) {
        var length = countersResponseEncoder.encodeIncrementResult(cacheId, result, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleCounterUpdated(cacheId, egressBuffer, length);
    }

    /**
     * Handle a delete cache message.
     *
     * @param session  Session requesting the delete operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder
     */
    <VT extends Reusable> void handleDeleteCache(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> responseEncoder, CacheManager<I, K, VT> cacheManager, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var requestDetails = getDeleteCacheRequestDetails(session, buffer, offset, decoder, deleteCacheRequestDetails);
        tracingService.startHandleDeleteCache(requestDetails);
        I cacheId = requestDetails.getCacheId();
        log.info("DELETE CACHE ID ON REQUEST {}", cacheId);
        var deleteCacheResult = processDeleteCache(cacheId, requestDetails.getRequestId(), cacheManager);
        log.info("DELETE RESULT CACHE ID {}", deleteCacheResult.getCacheId());
        handlePostDeleteCache(cacheId, deleteCacheResult, session, responseEncoder, subscriptionService);
        tracingService.endHandleDeleteCache(requestDetails);
    }

    private <VT extends Reusable> DeleteCacheResult<I> processDeleteCache(I cacheId, String requestId, CacheManager<I, K, VT> cacheManager) {
        log.info("Got delete cache request for cache id {}, request id {}", cacheId, requestId);
        var res = cacheManager.deleteCache(cacheId);
        res.setRequestId(requestId);
        return res;
    }

    /**
     * Handle a cache clear request.
     *
     * @param session  Session requesting the clear operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder
     */
    <VT extends Reusable> void handleClearCache(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> responseEncoder, CacheManager<I, K, VT> cacheManager, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var requestDetails = getClearCacheRequestDetails(session, buffer, offset, decoder, clearCacheRequestDetails);
        tracingService.startHandleClearCache(requestDetails);
        I cacheId = requestDetails.getCacheId();
        var requestId = requestDetails.getRequestId();
        var clearCacheResult = processClearCache(cacheId, requestId, cacheManager);
        handlePostClearCache(cacheId, clearCacheResult, session, responseEncoder, subscriptionService);
        tracingService.endHandleClearCache(requestDetails);
    }

    private <VT extends Reusable> ClearCacheResult<I> processClearCache(I cacheId, String requestId, CacheManager<I, K, VT> cacheManager) {
        log.info("Got clear cache request for cache id {} with requestId {}", cacheId, requestId);
        var clearCacheResult = cacheManager.clearCache(cacheId);
        clearCacheResult.setRequestId(requestId);
        return clearCacheResult;
    }

    /**
     * Handle a request to remove a cache entry.
     *
     * @param session  Session requesting the remove cache operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder
     */
    <VT extends Reusable> void handleRemoveCacheEntry(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> responseEncoder, CacheManager<I, K, VT> cacheManager, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var requestDetails = getRemoveCacheEntryRequestDetails(session, buffer, offset, decoder, removeCacheEntryRequestDetails);
        tracingService.startRemoveCacheEntry(requestDetails);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        var requestId = requestDetails.getRequestId();
        var removeCacheEntryResult = processRemoveCacheEntry(cacheId, key, requestId, cacheManager);
        handlePostRemoveCacheEntry(cacheId, key, removeCacheEntryResult, session, responseEncoder, subscriptionService);
        tracingService.endRemoveCacheEntry(requestDetails);
    }

    private <VT extends Reusable> RemoveCacheEntryResult<I, K> processRemoveCacheEntry(I cacheId, K key, String requestId, CacheManager<I, K, VT> cacheManager) {
        log.info("Got remove cache entry request for cache id {}, key {}, request Id: {}", cacheId, key, requestId);
        var removeCacheEntryResult = cacheManager.removeCacheEntry(cacheId, key);
        removeCacheEntryResult.setRequestId(requestId);
        return removeCacheEntryResult;
    }

    <VT extends Reusable> void handleCancelItemRemoval(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> responseEncoder, CacheTimerService<I, K> timerService) {
        decoder.decodeCancelItemRemovalRequest(buffer, offset, cancelItemRemovalRequestDetails);
        I cacheId = cancelItemRemovalRequestDetails.getCacheId();
        K key = cancelItemRemovalRequestDetails.getKey();
        var requestId = cancelItemRemovalRequestDetails.getRequestId();
        var result = processCancelItemRemoval(cacheId, key, requestId, timerService);
        var length = responseEncoder.encodeItemRemovalCancelled(cacheId, key, result, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    private CancelItemRemovalResult<I, K> processCancelItemRemoval(I cacheId, K key, String requestId, CacheTimerService<I, K> timerService) {
        log.info("Got cancel item removal request for cache id {}, key {}, request Id: {}", cacheId, key, requestId);
        cancelItemRemovalResult.clear();
        cancelItemRemovalResult.getCacheId().copyFrom(cacheId);
        cancelItemRemovalResult.getKey().copyFrom(key);
        cancelItemRemovalResult.setRequestId(requestId);
        boolean cancelled = timerService.cancelItemRemoval(cacheId, key);
        cancelItemRemovalResult.setCancelled(cancelled);
        cancelItemRemovalResult.setStatus(cancelled ? CacheOperationStatus.SUCCESS : CacheOperationStatus.UNKNOWN_KEY);
        return cancelItemRemovalResult;
    }

    /**
     * Handle a request to patch the value of a cache entry. The existing value and the
     * supplied patch are both treated as JSON, and the patch is merged into the existing value.
     *
     * @param session  Session requesting the patch operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder  The request decoder to use.
     */
    <VT extends Reusable> void handlePatchValue(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> responseEncoder, PatchValueRequestDetails<I, K, VT> patchValueRequestDetails, CacheManager<I, K, VT> cacheManager, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var requestDetails = getPatchValueRequestDetails(session, buffer, offset, decoder, patchValueRequestDetails);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        VT patch = requestDetails.getValue();
        var requestId = requestDetails.getRequestId();
        var patchValueResult = processPatchValue(cacheId, key, patch, requestId, cacheManager);
        handlePostPatchValue(cacheId, key, patchValueResult, session, responseEncoder, subscriptionService);
    }

    private <VT extends Reusable> PatchValueResult<I, K, VT> processPatchValue(I cacheId, K key, VT patch, String requestId, CacheManager<I, K, VT> cacheManager) {
        log.info("Got patch value request for cache id {}, key {}, request Id: {}", cacheId, key, requestId);
        var patchValueResult = cacheManager.patchValue(cacheId, key, patch);
        patchValueResult.setRequestId(requestId);
        return patchValueResult;
    }

    /**
     * Handle a request to add an entry to a cache.
     *
     * @param session            Session requesting the add entry operation.
     * @param buffer             Buffer containing the message.
     * @param offset             Offset in the buffer at which the message is encoded.
     * @param cacheTimerService_
     */
    <VT extends Reusable> void handleAddCacheEntry(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, CacheResponseEncoder<I, K, VT> responseEncoder, AddCacheEntryRequestDetails<I, K, VT> addCacheEntryRequestDetails, CacheManager<I, K, VT> cacheManager, CacheSubscriptionService<I, K, VT> subscriptionService, CacheTimerService<I, K> cacheTimerService_) {
        AddCacheEntryRequestDetails<I, K, VT> requestDetails = getAddCacheEntryRequestDetails(session, buffer, offset, decoder, addCacheEntryRequestDetails);
        tracingService.startAddCacheEntry(requestDetails);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        VT value = requestDetails.getValue();
        long ttl = requestDetails.getTtl();
        var requestId = requestDetails.getRequestId();
        var addCacheEntryResult = processAddCacheEntry(cacheId, key, value, ttl, requestId, session, buffer, offset, responseEncoder, cacheManager, subscriptionService, cacheTimerService_);

        log.info("Result for add entry, key: {}, status: {}, ", addCacheEntryResult.getEntryKey(), addCacheEntryResult.getStatus());
        handlePostAddCacheEntry(cacheId, key, value, addCacheEntryResult, session, responseEncoder, subscriptionService);
        tracingService.endAddCacheEntry(requestDetails);
    }

    private <VT extends Reusable> AddCacheEntryResult<I,K> processAddCacheEntry(I cacheId, K key, VT value, long ttl, String requestId, ClientSession session, DirectBuffer buffer, int offset, CacheResponseEncoder<I, K, VT> responseEncoder, CacheManager<I, K, VT> cacheManager, CacheSubscriptionService<I, K, VT> subscriptionService, CacheTimerService<I, K> timerService) {
        log.info("Got add cache entry request for cache id {}, key {}, value {}, ttl {}, request Id: {}", cacheId, key, value, ttl, requestId);
        var cache = cacheManager.getCache(cacheId);

        if (cache == null) {
            if (dynamicCacheCreationEnabled) {
                var createCacheResult = cacheManager.createCache(cacheId);
                log.info("Created cache {} dynamically, result {}", cacheId, createCacheResult);
                cache = cacheManager.getCache(cacheId);
            } else {
                if(session!=null){
                    handleMissingCacheOnAddEntry(session, buffer, offset, cacheId, key, value, requestId, responseEncoder, subscriptionService);
                }
                addEntryFailureResult.clear();
                addEntryFailureResult.setRequestId(requestId);
                addEntryFailureResult.getCacheId().copyFrom(cacheId);
                addEntryFailureResult.getEntryKey().copyFrom(key);
                addEntryFailureResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
                return addEntryFailureResult;
            }
        }

        var addCacheEntryResult = cache.add(key, value);
        addCacheEntryResult.setRequestId(requestId);
        addCacheEntryResult.getCacheId().copyFrom(cacheId);

        if (ttl > 0 && addCacheEntryResult.getStatus() == CacheOperationStatus.SUCCESS) {
            long now = cluster.time();
            long deadline = now + ttl;
            timerService.scheduleItemRemoval(cacheId, key, cache, deadline);
        }

        return addCacheEntryResult;
    }

    /**
     * @param session   Session requesting the add entry operation.
     * @param buffer    Buffer containing the message.
     * @param offset    Offset in the buffer at which the message is encoded.
     * @param cacheId   The Cache ID.
     * @param key       The key we tried to add the entry on.
     * @param value     The value we tried to add against the key.
     * @param requestId The original request ID.
     */
    private <VT extends Reusable> void handleMissingCacheOnAddEntry(ClientSession session, DirectBuffer buffer, int offset, I cacheId, K key, VT value, String requestId, CacheResponseEncoder<I, K, VT> responseEncoder, CacheSubscriptionService<I, K, VT> subscriptionService) {
        addEntryFailureResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        addEntryFailureResult.setRequestId(requestId);
        addEntryFailureResult.getCacheId().copyFrom(cacheId);
        log.info("Cache {} doesn't exist, tried to add on key key: {}", cacheId, addEntryFailureResult.getEntryKey());

        handlePostAddCacheEntry(cacheId, key, value, addEntryFailureResult, session, responseEncoder, subscriptionService);
    }

    /**
     * Handle a request to get a cache entry.
     *
     * @param session  Session requesting the add entry operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder  The request decoder to use.
     */
    <VT extends Reusable> void handleGetCacheEntry(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> responseEncoder, CacheManager<I, K, VT> cacheManager) {
        var requestDetails = getCacheEntryRequestDetails(session, buffer, offset, decoder, getCacheEntryRequestDetails);
        tracingService.startGetCacheEntry(requestDetails);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        var requestId = requestDetails.getRequestId();
        var getCacheEntryResult = processGetCacheEntry(cacheId, key, requestId, cacheManager);
        log.info("Sending GET result: cacheId {}, key {}, value {}, reqId {}, status {}", cacheId, key, getCacheEntryResult.getEntryValue(), getCacheEntryResult.getRequestId(), getCacheEntryResult.getStatus());
        handlePostGetCacheEntry(cacheId, key, getCacheEntryResult, session, responseEncoder);
        tracingService.endGetCacheEntry(requestDetails);
    }

    private <VT extends Reusable> GetCacheEntryResult<I, K, VT> processGetCacheEntry(I cacheId, K key, String requestId, CacheManager<I, K, VT> cacheManager) {
        log.info("Got get cache entry for cache id {} on key {}, requestId {}", cacheId, key, requestId);
        var getCacheEntryResult = cacheManager.getCacheEntry(cacheId, key);
        getCacheEntryResult.setRequestId(requestId);
        //getCacheEntryResult.getCacheId().copyFrom(cacheId);
        return getCacheEntryResult;
    }

    /**
     * Handle a request to get all items from a cache.
     *
     * @param session  Session requesting the add entry operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder                   The request decoder to use.
     * @param encoder                   The response encoder to use.
     */
    private <VT extends Reusable> void handleGetAllCacheEntries(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> encoder, CacheManager<I, K, VT> cacheManager) {
        var requestDetails = getAllCacheEntriesRequestDetails(session, buffer, offset, decoder, getAllCacheEntriesRequestDetails);
        tracingService.startGetAllCacheEntries(requestDetails);
        I cacheId = requestDetails.getCacheId();
        var requestId = requestDetails.getRequestId();
        log.info("Got get cache content for cache id {}, requestId {}", cacheId, requestId);

        var getAllCacheEntriesResult = cacheManager.getAllCacheEntries(cacheId);
        getAllCacheEntriesResult.setRequestId(requestId);
        getAllCacheEntriesResult.getCacheId().copyFrom(cacheId);
        handlePostGetAllCacheEntries(cacheId, getAllCacheEntriesResult, session, encoder);
        tracingService.endGetAllCacheEntries(requestDetails);
    }

    /**
     * Handle a request to create a cache.
     *
     * @param session                   Session requesting the create cache operation.
     * @param buffer                    Buffer containing the message.
     * @param offset                    Offset in the buffer at which the message is encoded.
     * @param decoder                   The request decoder to use.
     * @param encoder                   The response encoder to use.
     * @param createCacheRequestDetails The flyweight to populate.
     */
    <VT extends Reusable> void handleCreateCache(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> encoder, CreateCacheRequestDetails<I> createCacheRequestDetails, CacheManager<I, K, VT> cacheManager) {
        CreateCacheRequestDetails<I> requestDetails = getCreateCacheRequestDetails(session, buffer, offset, decoder, createCacheRequestDetails);
        tracingService.startCreateCacheRequest(requestDetails);
        I cacheId = requestDetails.getCacheId();
        var requestId = requestDetails.getRequestId();
        var cacheCreationResult = processCreateCache(cacheId, requestId, cacheManager);
        log.info("Will send result: " + cacheCreationResult.getStatus());
        handlePostCreateCache(cacheId, cacheCreationResult, session, encoder);
        tracingService.endCreateCacheRequest(requestDetails);
    }

    private <VT extends Reusable> CreateCacheResult<I> processCreateCache(I cacheId, String requestId, CacheManager<I,K, VT> cacheManager) {
        log.info("Got create cache request for cache id {}, request Id: {}", cacheId, requestId);
        var cacheCreationResult = cacheManager.createCache(cacheId);
        cacheCreationResult.setRequestId(requestId);
        return cacheCreationResult;
    }

    /**
     * Handle a request to get all cache stats.
     *
     * @param session  Session requesting the get all stats operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder                   The request decoder to use.
     * @param encoder                   The response encoder to use.
     */
    <VT extends Reusable> void handleGetCacheStats(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> encoder, CacheManager<I, K, VT> cacheManager) {
        GetCacheStatsRequestDetails requestDetails = getCacheStatsRequestDetails(session, buffer, offset, decoder, getCacheStatsRequestDetails);
        tracingService.startGetAllStatsRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        log.info("Got request for all cache stats, request Id: {}", requestId);
        var cacheStatsResult = cacheManager.getCacheStatsResult();
        cacheStatsResult.setRequestId(requestId);
        handlePostGetCacheStats(cacheStatsResult, session, encoder);
        tracingService.endGetAllStatsRequest(requestDetails);
    }

    /**
     * Handle a request to subscribe to caches.
     *
     * @param session  Session requesting the get all stats operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder                   The request decoder to use.
     * @param encoder                   The response encoder to use.
     */
    <VT extends Reusable> void handleCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> encoder, CacheSubscriptionService<I,K, VT> subscriptionService, CacheManager<I,K, VT> cacheManager) {
        CacheSubscriptionRequestDetails<I> requestDetails = getCacheSubscriptionRequest(session, buffer, offset, decoder, cacheSubscribeRequestDetails);
        tracingService.startCacheSubscriptionRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        var cacheIds = requestDetails.getCacheId();

        log.info("Total caches to subscribe on: {}", cacheIds.size());

        for (int i = 0; i < cacheIds.size(); i++) {
            var cacheId = cacheIds.get(i);
            var result = subscriptionService.subscribe(session, cacheId, requestId);
            result.setEob(i==cacheIds.size()-1);

            log.info("Got request to subscribe for cache updates on cache: {}, request Id: {}, " +
                    "send snapshot: {}, is EOB: {}", cacheId, requestId, requestDetails.isSendSnapshot(), result.isEob());

            if (requestDetails.isSendSnapshot()) {
                log.info("Sending back snapshot for cache {}", cacheId);
                populateSnapshot(result, cacheId, cacheManager);
            } else {
                result.entries = null;
            }

            handlePostCacheSubscriptionRequest(result, session, encoder);
        }

        tracingService.endCacheSubscriptionRequest(requestDetails);
    }

    private <VT extends Reusable> void populateSnapshot(CacheSubscriptionResult<I,K,VT> result, I cacheId, CacheManager<I,K,VT> cacheManager) {
        Cache<I, K, VT> cache = cacheManager.getCache(cacheId);

        if (cache != null) {
            result.entries = cache.getAllEntries();
        } else {
            result.entries = null;
        }
    }

    /**
     * Handle a request to unsubscribe to a cache.
     *
     * @param session  Session requesting the get all stats operation.
     * @param buffer   Buffer containing the message.
     * @param offset   Offset in the buffer at which the message is encoded.
     * @param decoder                   The request decoder to use.
     * @param encoder                   The response encoder to use.
     */
    <VT extends Reusable> void handleCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I, K, VT> decoder, CacheResponseEncoder<I, K, VT> encoder, CacheSubscriptionService<I,K, VT> subscriptionService, CacheManager<I,K, VT> cacheManager) {
        CacheUnsubscribeRequestDetails<I> requestDetails = getCacheUnsubscribeRequest(session, buffer, offset, decoder, cacheUnsubscribeRequestDetails);
        tracingService.startCacheUnsubscribeRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        var cacheId = requestDetails.getCacheId();
        log.info("Got request to unsubscribe for cache updates on cache: {}, request Id: {}", cacheId, requestId);
        var result = subscriptionService.unsubscribe(requestDetails, session);
        handlePostCacheUnsubscribeRequest(result, session, encoder);
        tracingService.endCacheUnsubscribeRequest(requestDetails);
    }

    /**
     * Handle a bulk operation request.
     *
     * @param session                      Session requesting the get all stats operation.
     * @param buffer                       Buffer containing the message.
     * @param offset                       Offset in the buffer at which the message is encoded.
     * @param encoder                      The response encoder to use.
     * @param countersSubscriptionService_
     * @param cacheValueSupplier
     * @param counterCacheValueSupplier
     * @param cacheTimerService_
     * @param cacheCountersTimerService_
     */
    void handleBulkOpsRequest(ClientSession session, DirectBuffer buffer, int offset,
                              CacheManager<I, K, V> cacheManager, CountersCacheManager<I, K, ReusableLong> countersCacheManager_,
                              CacheResponseEncoder<I, K, V> encoder,
                              CacheResponseEncoder<I, K, ReusableLong> countersEncoder,
                              CacheSubscriptionService<I, K, V> subscriptionService,
                              CacheSubscriptionService<I, K, ReusableLong> countersSubscriptionService_,
                              Supplier<V> cacheValueSupplier, Supplier<ReusableLong> counterCacheValueSupplier,
                              CacheTimerService<I, K> cacheTimerService_, CacheTimerService<I, K> cacheCountersTimerService_) {
        BulkCacheOpsRequestDetails<I,K,V> requestDetails = getBulkOpsRequest(session, buffer, offset, bulkCacheOpsRequestDetails);
        tracingService.startBulkOpsRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        log.info("Got bulk operations request with Id: {}", requestId);
        BulkCacheOpsResult<I,K,V> res = processBulkOperations(requestDetails, cacheManager, countersCacheManager_, encoder, countersEncoder, subscriptionService, countersSubscriptionService_, cacheValueSupplier, counterCacheValueSupplier, cacheTimerService_, cacheCountersTimerService_);
        res.setRequestId(requestId);

        var length = encoder.encodeBulkOpsResponse(res, egressBuffer);
        sendMessage(session, egressBuffer, length);

        tracingService.endBulkOpsRequest(requestDetails);
    }

    private BulkCacheOpsResult<I,K,V> processBulkOperations(BulkCacheOpsRequestDetails<I,K,V> requestDetails,
                                                            CacheManager<I, K, V> cacheManager, CountersCacheManager<I, K, ReusableLong> countersCacheManager,
                                                            CacheResponseEncoder<I, K, V> encoder_,
                                                            CacheResponseEncoder<I, K, ReusableLong> countersEncoder,
                                                            CacheSubscriptionService<I, K, V> subscriptionService,
                                                            CacheSubscriptionService<I, K, ReusableLong> countersSubscriptionService,
                                                            Supplier<V> cacheValueSupplier,
                                                            Supplier<ReusableLong> counterCacheValueSupplier,
                                                            CacheTimerService<I, K> cacheTimerService_, CacheTimerService<I, K> cacheCountersTimerService_) {

        bulkOpsResult.clear();

        for (var op : requestDetails.getOperations()) {
            switch (op.getOperationType()) {
                case CREATE_CACHE -> handleBulkOpCreateCache(op, bulkOpsResult, cacheManager);
                case ADD_ITEM -> handleBulkOpAddItem(op, bulkOpsResult, op.getValue(), cacheManager, encoder_, subscriptionService, cacheTimerService_);
                case CLEAR_CACHE -> handleBulkOpClearCache(op, bulkOpsResult, cacheManager, encoder_, subscriptionService);
                case GET_ITEM -> handleBulkOpGetItem(op, bulkOpsResult, cacheManager, cacheValueSupplier);
                case DELETE_CACHE -> handleBulkOpDeleteCache(op, bulkOpsResult, cacheManager, encoder_, subscriptionService);
                case REMOVE_ITEM -> handleBulkOpRemoveItem(op, bulkOpsResult, cacheManager, encoder_, subscriptionService);
                case CANCEL_ITEM -> handleBulkOpCancelItem(op, bulkOpsResult, cacheTimerService_);
                case PATCH_ITEM -> handleBulkOpPatchItem(op, bulkOpsResult, op.getValue(), cacheManager, encoder_, subscriptionService, cacheValueSupplier);

                case CREATE_COUNTER_CACHE -> handleBulkOpCreateCache(op, bulkOpsResult, countersCacheManager);
                case ADD_COUNTER -> handleBulkOpAddCounter(op, bulkOpsResult, countersCacheManager, countersEncoder, countersSubscriptionService, counterCacheValueSupplier, cacheCountersTimerService_);
                case CLEAR_COUNTER_CACHE -> handleBulkOpClearCache(op, bulkOpsResult, countersCacheManager, countersEncoder, countersSubscriptionService);
                case GET_COUNTER -> handleBulkOpGetItem(op, bulkOpsResult, countersCacheManager, counterCacheValueSupplier);
                case DELETE_COUNTER_CACHE -> handleBulkOpDeleteCache(op, bulkOpsResult, countersCacheManager, countersEncoder, countersSubscriptionService);
                case REMOVE_COUNTER -> handleBulkOpRemoveItem(op, bulkOpsResult, countersCacheManager, countersEncoder, countersSubscriptionService);
                case CANCEL_COUNTER -> handleBulkOpCancelItem(op, bulkOpsResult, cacheCountersTimerService_);

                case INCREMENT_COUNTER -> handleBulkOpIncrementCounter(op, bulkOpsResult, countersCacheManager);
                case DECREMENT_COUNTER -> handleBulkOpDecrementCounter(op, bulkOpsResult, countersCacheManager);
                case SET_COUNTER -> handleBulkOpSetCounter(op, bulkOpsResult, countersCacheManager);

                case NONE -> log.warn("Ignoring bulk operation with type NONE for request {}", op.getRequestId());
            }
        }

        return bulkOpsResult;
    }

    private void handleBulkOpDecrementCounter(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        I cacheId = op.getCacheId();
        K counterId = op.getKey();
        long amount = op.getCounterValue();
        var requestId = op.getRequestId();
        var result = processDecrementCounter(cacheId, counterId, amount, requestId, countersCacheManager);
        DecrementCounterResult<I, K> bulkResult = new DecrementCounterResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, decrement counter result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);
    }

    private void handleBulkOpIncrementCounter(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        I cacheId = op.getCacheId();
        K counterId = op.getKey();
        long amount = op.getCounterValue();
        var requestId = op.getRequestId();
        var result = processIncrementCounter(cacheId, counterId, amount, requestId, countersCacheManager);
        IncrementCounterResult<I, K> bulkResult = new IncrementCounterResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, increment counter result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);
    }

    private <VT extends Reusable> void handleBulkOpRemoveItem(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CacheManager<I, K, VT> cacheManager, CacheResponseEncoder<I, K, VT> encoder_, CacheSubscriptionService<I, K, VT> subscriptionService) {
        I cacheId = op.getCacheId();
        var requestId = op.getRequestId();
        K key = op.getKey();
        var result = processRemoveCacheEntry(cacheId, key, requestId, cacheManager);

        RemoveCacheEntryResult<I, K> bulkResult = new RemoveCacheEntryResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, remove item result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);

        if (result.getStatus() == CacheOperationStatus.SUCCESS) {
            // update the subscription service
            handlePostRemoveCacheEntry(cacheId, key, result, null, encoder_, subscriptionService);
        }
    }

    private void handleBulkOpCancelItem(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CacheTimerService<I, K> timerService) {
        I cacheId = op.getCacheId();
        K key = op.getKey();
        var requestId = op.getRequestId();
        var result = processCancelItemRemoval(cacheId, key, requestId, timerService);
        CancelItemRemovalResult<I, K> bulkResult = new CancelItemRemovalResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, cancel item removal result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);
    }

    private void handleBulkOpSetCounter(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CountersCacheManager<I, K, ReusableLong> countersCacheManager) {
        I cacheId = op.getCacheId();
        K counterId = op.getKey();
        long counterValue = op.getCounterValue();
        var requestId = op.getRequestId();
        var result = processSetCounter(cacheId, counterId, counterValue, requestId, countersCacheManager);
        SetCounterResult<I, K> bulkResult = new SetCounterResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, set counter result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);
    }

    private <VT extends Reusable> void handleBulkOpPatchItem(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, VT patch, CacheManager<I, K, VT> cacheManager, CacheResponseEncoder<I, K, VT> encoder_, CacheSubscriptionService<I, K, VT> subscriptionService, Supplier<VT> valueSupplier) {
        I cacheId = op.getCacheId();
        K key = op.getKey();
        var requestId = op.getRequestId();
        var result = processPatchValue(cacheId, key, patch, requestId, cacheManager);
        PatchValueResult<I, K, VT> bulkResult = new PatchValueResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get(), valueSupplier.get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, patch item result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);

        // update the subscription service
        if (result.getStatus() == CacheOperationStatus.SUCCESS) {
            handlePostPatchValue(cacheId, key, result, null, encoder_, subscriptionService);
        }
    }

    private <VT extends Reusable> void handleBulkOpDeleteCache(CacheOperationRequestDetails<I,K,V> op, BulkCacheOpsResult<I,K,V> bulkOpsResult, CacheManager<I, K, VT> cacheManager, CacheResponseEncoder<I, K, VT> encoder_, CacheSubscriptionService<I, K, VT> subscriptionService) {
        I cacheId = op.getCacheId();
        var result = processDeleteCache(cacheId, op.getRequestId(), cacheManager);
        DeleteCacheResult<I> bulkResult = new DeleteCacheResult<>(cacheManagerFactory.getIndexSupplier().get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, delete cache result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);

        // update the subscription service
        if (result.getStatus() == CacheOperationStatus.SUCCESS) {
            handlePostDeleteCache(cacheId, result, null, encoder_, subscriptionService);
        }
    }

    private <VT extends Reusable> void handleBulkOpGetItem(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CacheManager<I, K, VT> cacheManager, Supplier<VT> valueSupplier) {
        I cacheId = op.getCacheId();
        var requestId = op.getRequestId();
        K key = op.getKey();
        GetCacheEntryResult<I, K, VT> result = processGetCacheEntry(cacheId, key, requestId, cacheManager);
        GetCacheEntryResult<I,K,VT> bulkResult = new GetCacheEntryResult<>(cacheManagerFactory.getIndexSupplier().get(),
                cacheManagerFactory.getKeySupplier().get(), valueSupplier.get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, get item result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);
    }

    private <VT extends Reusable> void handleBulkOpClearCache(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CacheManager<I, K, VT> cacheManager, CacheResponseEncoder<I, K, VT> encoder_, CacheSubscriptionService<I, K, VT> subscriptionService) {
        I cacheId = op.getCacheId();
        var requestId = op.getRequestId();
        var result = processClearCache(cacheId, requestId, cacheManager);
        ClearCacheResult<I> bulkResult = new ClearCacheResult<>(cacheManagerFactory.getIndexSupplier().get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, clear cache result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);

        // update the subscription service
        if (result.getStatus() == CacheOperationStatus.SUCCESS) {
            handlePostClearCache(cacheId, result, null, encoder_, subscriptionService);
        }
    }

    private <VT extends Reusable> void handleBulkOpAddItem(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, VT value, CacheManager<I, K, VT> cacheManager, CacheResponseEncoder<I, K, VT> encoder_, CacheSubscriptionService<I, K, VT> subscriptionService, CacheTimerService<I, K> cacheTimerService_) {
        I cacheId = op.getCacheId();
        K key = op.getKey();
        var ttl = op.getTtl();
        var requestId = op.getRequestId();
        var result = processAddCacheEntry(cacheId, key, value, ttl, requestId, null, null, 0, encoder_, cacheManager, subscriptionService, cacheTimerService_);
        AddCacheEntryResult<I, K> bulkResult = new AddCacheEntryResult<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        bulkResult.copyFrom(result);
        log.info("Bulk request, add item result: {}, value: {}", bulkResult, value);
        bulkOpsResult.addResult(bulkResult);

        // update the subscription service
        if (result.getStatus() == CacheOperationStatus.SUCCESS) {
            handlePostAddCacheEntry(cacheId, key, value, result, null, encoder_, subscriptionService);
        }
    }

    private void handleBulkOpAddCounter(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CountersCacheManager<I, K, ReusableLong> countersCacheManager, CacheResponseEncoder<I, K, ReusableLong> encoder_, CacheSubscriptionService<I, K, ReusableLong> subscriptionService, Supplier<ReusableLong> counterCacheValueSupplier, CacheTimerService<I, K> cacheCountersTimerService_) {
        ReusableLong value = counterCacheValueSupplier.get();
        value.copyFrom(op.getCounterValue());

        log.info("Request counter value: {}", op.getCounterValue());

        handleBulkOpAddItem(op, bulkOpsResult, value, countersCacheManager, encoder_, subscriptionService, cacheCountersTimerService_);
    }

    private <VT extends Reusable> void handleBulkOpCreateCache(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult, CacheManager<I, K, VT> cacheManager) {
        I cacheId = op.getCacheId();
        var requestId = op.getRequestId();
        var result = processCreateCache(cacheId, requestId, cacheManager);
        CreateCacheResult<I> bulkResult = new CreateCacheResult<>(cacheManagerFactory.getIndexSupplier().get());
        bulkResult.copyFrom(result);
        log.debug("Bulk request, create cache result: {}", bulkResult);
        bulkOpsResult.addResult(bulkResult);
    }

    /**
     * Decode the CreateCache message into a request details flyweight.
     *
     * @param createCacheRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The CreateCacheRequestDetails flyweight.
     */
    protected <VT extends Reusable> CreateCacheRequestDetails<I> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, CreateCacheRequestDetails<I> createCacheRequestDetails) {
        decoder.decodeGetCreateCacheRequestDetails(buffer, offset, createCacheRequestDetails);
        return createCacheRequestDetails;
    }

    /**
     * Decode the ClearCache message into a request details flyweight.
     *
     * @param clearCacheRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The ClearCacheRequestDetails flyweight.
     */
    protected <VT extends Reusable> ClearCacheRequestDetails<I> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, ClearCacheRequestDetails<I> clearCacheRequestDetails) {
        decoder.decodeClearCacheRequest(buffer, offset, clearCacheRequestDetails);
        return clearCacheRequestDetails;
    }

    /**
     * Decode the RemoveCacheEntry message into a request details flyweight.
     *
     * @param removeCacheEntryRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The RemoveCacheEntryRequestDetails flyweight.
     */
    protected <VT extends Reusable> RemoveCacheEntryRequestDetails<I, K> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, RemoveCacheEntryRequestDetails<I, K> removeCacheEntryRequestDetails) {
        decoder.decodeRemoveCacheEntryRequest(buffer, offset, removeCacheEntryRequestDetails);
        return removeCacheEntryRequestDetails;
    }

    /**
     * Decode the PatchCacheEntry message into a request details flyweight.
     *
     * @param patchValueRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The PatchValueRequestDetails flyweight.
     */
    protected <VT extends Reusable> PatchValueRequestDetails<I, K, VT> getPatchValueRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, PatchValueRequestDetails<I, K, VT> patchValueRequestDetails) {
        decoder.decodePatchValueRequest(buffer, offset, patchValueRequestDetails);
        return patchValueRequestDetails;
    }

    /**
     * Decode the AddCacheEntry message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The AddCacheEntryRequestDetails flyweight.
     */
    protected <VT extends Reusable> AddCacheEntryRequestDetails<I, K, VT> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, AddCacheEntryRequestDetails<I, K, VT> addCacheEntryRequestDetails) {
        decoder.decodeAddCacheEntryRequest(buffer, offset, addCacheEntryRequestDetails);
        return addCacheEntryRequestDetails;
    }

    /**
     * Decode the GetCacheEntry message into a request details flyweight.
     *
     * @param getCacheEntryRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The GetCacheEntryRequestDetails flyweight.
     */
    protected <VT extends Reusable> GetCacheEntryRequestDetails<I, K> getCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, GetCacheEntryRequestDetails<I, K> getCacheEntryRequestDetails) {
        decoder.decodeGetCacheEntryRequest(buffer, offset, getCacheEntryRequestDetails);
        return getCacheEntryRequestDetails;
    }

    /**
     * Decode the GetAllCacheEntries message into a request details flyweight.
     *
     * @param getAllCacheEntriesRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The GetAllCacheEntriesRequestDetails flyweight.
     */
    protected <VT extends Reusable> GetAllCacheEntriesRequestDetails<I> getAllCacheEntriesRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, GetAllCacheEntriesRequestDetails<I> getAllCacheEntriesRequestDetails) {
        decoder.decodeGetAllCacheEntriesRequest(buffer, offset, getAllCacheEntriesRequestDetails);
        return getAllCacheEntriesRequestDetails;
    }

    /**
     * Decode the DeleteCache message into a request details flyweight.
     *
     * @param deleteCacheRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The DeleteCacheRequestDetails flyweight.
     */
    protected <VT extends Reusable> DeleteCacheRequestDetails<I> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, DeleteCacheRequestDetails<I> deleteCacheRequestDetails) {
        decoder.decodeGetDeleteCacheRequest(buffer, offset, deleteCacheRequestDetails);
        return deleteCacheRequestDetails;
    }

    /**
     * Decode the CacheStatsRequest message into a request details flyweight.
     *
     * @param getCacheStatsRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The GetCacheStatsRequestDetails flyweight.
     */
    protected <VT extends Reusable> GetCacheStatsRequestDetails getCacheStatsRequestDetails(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, GetCacheStatsRequestDetails getCacheStatsRequestDetails) {
        decoder.decodeGetCacheStatsRequest(buffer, offset, getCacheStatsRequestDetails);
        return getCacheStatsRequestDetails;
    }

    /**
     * Decode the CacheSubscriptionRequest message into a request details flyweight.
     *
     * @param cacheSubscribeRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The CacheSubscriptionRequestDetails flyweight.
     */
    protected <VT extends Reusable> CacheSubscriptionRequestDetails<I> getCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails) {
        decoder.decodeCacheSubscriptionRequest(buffer, offset, cacheSubscribeRequestDetails);
        return cacheSubscribeRequestDetails;
    }

    /**
     * Decode the CacheUnsubscribeRequest message into a request details flyweight.
     *
     * @param cacheUnsubscribeRequestDetails The request details flyweight to populate.
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The CacheUnsubscribeRequestDetails flyweight.
     */
    protected <VT extends Reusable> CacheUnsubscribeRequestDetails<I> getCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset, CacheRequestDecoder<I,K,VT> decoder, CacheUnsubscribeRequestDetails<I> cacheUnsubscribeRequestDetails) {
        decoder.decodeGetCacheUnsubscribeRequest(buffer, offset, cacheUnsubscribeRequestDetails);
        return cacheUnsubscribeRequestDetails;
    }


    /**
     * Decode the BulkCacheOpsRequestDetails message into a request details flyweight.
     *
     * @param session                     The client session.
     * @param buffer                      The buffer to decode from.
     * @param offset                      The offset from within the buffer to decode from.
     * @param bulkCacheOpsRequestDetails_
     * @return The BulkCacheOpsRequestDetails flyweight.
     */
    protected <VT extends Reusable> BulkCacheOpsRequestDetails<I, K, VT> getBulkOpsRequest(ClientSession session, DirectBuffer buffer, int offset, BulkCacheOpsRequestDetails<I, K, VT> bulkCacheOpsRequestDetails_) {
        decoder.decodeBulkCacheOperationsRequest(buffer, offset, bulkCacheOpsRequestDetails);
        return bulkCacheOpsRequestDetails_;
    }

    /**
     * After the cache is created, send out a CacheCreated message.
     *
     * @param cacheId             The ID of the cache created.
     * @param cacheCreationResult The result from the request to create the cache.
     * @param session             The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostCreateCache(I cacheId, CreateCacheResult<I> cacheCreationResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder) {
        var length = encoder.encodeCacheCreationResult(cacheId, cacheCreationResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    /**
     * After an entry is added to a cache, send out a EntryCreated message.
     *
     * @param cacheId             The ID of the cache in which the entry was created.
     * @param addCacheEntryResult The result from the request to add an entry.
     * @param session             The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostAddCacheEntry(I cacheId, K key, VT value, AddCacheEntryResult<I, K> addCacheEntryResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var addCacheEntryResultLength = encoder.encodeAddCacheEntryResult(cacheId, key, addCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, addCacheEntryResultLength);

        var entryUpdatedLength = encoder.encodeEntryUpdated(key, value, addCacheEntryResult, egressBuffer);
        subscriptionService.handleEntryAdded(addCacheEntryResult, egressBuffer, key, value, entryUpdatedLength);
    }

    /**
     * Get an entry from the cache, send out a CacheEntry message.
     *
     * @param cacheId             The ID of the cache we need to get the entry from.
     * @param getCacheEntryResult The result from the request to add an entry.
     * @param session             The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostGetCacheEntry(I cacheId, K key, GetCacheEntryResult<I, K, VT> getCacheEntryResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder) {
        var length = encoder.encodeCacheEntryResult(cacheId, getCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    /**
     * After an entry is patched, send out a CacheEntryPatched message reporting the outcome.
     *
     * @param cacheId          The ID of the cache in which the entry was patched.
     * @param key              The key of the entry that was patched.
     * @param patchValueResult The result from the request to patch the entry.
     * @param session          The client session.
     * @param encoder          The response encoder to use.
     * @param subscriptionService The subscription service used to notify subscribers of the new value.
     */
    protected <VT extends Reusable> void handlePostPatchValue(I cacheId, K key, PatchValueResult<I, K, VT> patchValueResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var length = encoder.encodePatchValueResult(cacheId, patchValueResult, egressBuffer);
        sendMessage(session, egressBuffer, length);

        if (patchValueResult.getStatus() == CacheOperationStatus.SUCCESS) {
            VT value = patchValueResult.getEntryValue();
            patchEntryUpdateResult.clear();
            patchEntryUpdateResult.setRequestId(patchValueResult.getRequestId());
            patchEntryUpdateResult.getCacheId().copyFrom(cacheId);
            patchEntryUpdateResult.getEntryKey().copyFrom(key);
            patchEntryUpdateResult.setStatus(CacheOperationStatus.SUCCESS);

            var entryUpdatedLength = encoder.encodeEntryUpdated(key, value, patchEntryUpdateResult, egressBuffer);
            subscriptionService.handleEntryAdded(patchEntryUpdateResult, egressBuffer, key, value, entryUpdatedLength);
        }
    }

    /**
     * Get all entries from the cache.
     *
     * @param cacheId             The ID of the cache we need to get all entries from.
     * @param getAllCacheEntriesResult The result from the request to get all entries.
     * @param session             The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostGetAllCacheEntries(I cacheId, GetAllCacheEntriesResult<I, K, VT> getAllCacheEntriesResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder) {
        var length = encoder.encodeAllCacheEntriesResult(cacheId, getAllCacheEntriesResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    /**
     * After an entry is removed from the cache, send out a EntryRemoved message.
     *
     * @param cacheId                The ID of the cache in which the entry was removed.
     * @param removeCacheEntryResult The result from the request to remove an entry.
     * @param session                The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostRemoveCacheEntry(I cacheId, K key, RemoveCacheEntryResult<I, K> removeCacheEntryResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var length = encoder.encodeRemoveCacheEntryResult(cacheId, key, removeCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleEntryRemoved(removeCacheEntryResult, egressBuffer, length, session!=null ? session.id() : Long.MAX_VALUE);
    }

    /**
     * After an entry is removed from the cache on a timer event.
     *
     * @param cacheId                The ID of the cache in which the entry was removed.
     * @param key
     * @param removeCacheEntryResult The result from the request to remove an entry.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostRemoveTimerCacheEntry(I cacheId, K key, RemoveCacheEntryResult<I, K> removeCacheEntryResult, CacheResponseEncoder<I, K, VT> encoder, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var length = encoder.encodeRemoveCacheEntryResult(cacheId, key, removeCacheEntryResult, egressBuffer);
        subscriptionService.handleTimerEntryRemoved(removeCacheEntryResult, egressBuffer, length);
    }

    /**
     * After a cache is cleared, send out a CacheCleared message.
     *
     * @param cacheId          The ID of the cache in which the entry was removed.
     * @param clearCacheResult The result from the request to clear a cache.
     * @param session          The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostClearCache(I cacheId, ClearCacheResult<I> clearCacheResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var length = encoder.encodeCacheCleared(cacheId, clearCacheResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleClearCache(clearCacheResult, egressBuffer, length, session!=null ? session.id() : Long.MAX_VALUE);
    }

    /**
     * After a cache is deleted, send out a CacheDeleted message.
     *
     * @param cacheId           The ID of the cache which was deleted.
     * @param deleteCacheResult The result of deleting the cache.
     * @param session           The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostDeleteCache(I cacheId, DeleteCacheResult<I> deleteCacheResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder, CacheSubscriptionService<I, K, VT> subscriptionService) {
        var length = encoder.encodeDeleteCache(cacheId, deleteCacheResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleDeleteCache(deleteCacheResult, egressBuffer, length, session!=null ? session.id() : Long.MAX_VALUE);
    }

    /**
     * Send out the cache stats.
     *
     * @param cacheStatsResult The stats across all caches.
     * @param session          The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostGetCacheStats(CacheStatsResult<I> cacheStatsResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder) {
        var length = encoder.encodeCacheStatsResult(cacheStatsResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    /**
     * Send out the result of subscribing to a cache.
     *
     * @param subscriptionRequestResult The result of subscribing.
     * @param session                   The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<I,K,VT> subscriptionRequestResult, ClientSession session, CacheResponseEncoder<I, K, VT> encoder) {
        encoder.encodeCacheSubscriptionResult(subscriptionRequestResult, egressBuffer, keyComparator, new HydratingPublicationConsumer() {
            @Override
            public void accept(MutableDirectBuffer mutableDirectBuffer) {
                var l = this.getLength();
                sendMessage(session, mutableDirectBuffer, l);
            }
        });
    }

    /**
     * Send out the result of unsubscribing to a cache.
     *
     * @param unsubscribeResponse The result of unsubscribing.
     * @param session             The client session.
     * @param encoder
     */
    protected <VT extends Reusable> void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<I> unsubscribeResponse, ClientSession session, CacheResponseEncoder<I, K, VT> encoder) {
        var length = encoder.encodeCacheUnsubscribeResponse(unsubscribeResponse, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    /**
     * Send out the result of bulk operations done on the cache.
     *
     * @param bulkCacheOpsResult The result of the bulk operation.
     * @param session            The client session.
     * @param encoder
     */
    protected void handlePostBulkOpsRequest(BulkCacheOpsResult<I, K, V> bulkCacheOpsResult, ClientSession session, CacheResponseEncoder<I, K, V> encoder) {
        var length = encoder.encodeBulkOpsResponse(bulkCacheOpsResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }


    /**
     * @param session   Session to send the message too.
     * @param msgBuffer The buffer containing the message.
     * @param len       The length of the message.
     */
    void sendMessage(final ClientSession session, MutableDirectBuffer msgBuffer, int len) {
        long offered = 0;
        while (session!=null && (offered = session.offer(msgBuffer, 0, len)) < 0) {
            publicationFailureHandler.handleOfferFailure(offered);
            idleStrategy.idle();
        }
    }

    /**
     * Record the state to a publication.
     *
     * @param snapshotPublication to which the state should be recorded.
     */
    @Override
    public void onTakeSnapshot(final ExclusivePublication snapshotPublication) {
        MutableDirectBuffer timersBuffer = new ExpandableArrayBuffer();

        log.info("Taking timers service snapshot");
        int cumulativeLength = cacheTimerService.onTakeSnapshot(snapshotPublication, timersBuffer);
        snapshotPublication.offer(timersBuffer, 0, cumulativeLength);

        cumulativeLength = cacheCountersTimerService.onTakeSnapshot(snapshotPublication, timersBuffer);
        snapshotPublication.offer(timersBuffer, 0, cumulativeLength);

        log.info("Taking cache manager snapshot");
        cacheManager.takeSnapshot(snapshotPublication);

        log.info("Taking counters cache manager snapshot");
        countersCacheManager.takeSnapshot(snapshotPublication);
    }

    /**
     * Load the state from an Image.
     *
     * @param cluster       The cluster from which we are loading the snapshot.
     * @param snapshotImage The snapshot image.
     */
    void loadSnapshot(final Cluster cluster, final Image snapshotImage) {

        log.info("Loading cache timers from snapshot");
        cacheTimerService.loadSnapshot(cluster, snapshotImage);

        log.info("Loading cache counter timers from snapshot");
        cacheCountersTimerService.loadSnapshot(cluster, snapshotImage);

        log.info("Loading cache manager snapshot");
        cacheManager.loadSnapshot(snapshotImage);

        log.info("Loading counters cache manager snapshot");
        countersCacheManager.loadSnapshot(snapshotImage);
    }

    /**
     * @param newRole that the node has assumed.
     */
    public void onRoleChange(final Cluster.Role newRole) {
        log.info("Node {} has new role of {}", nodeId, newRole);
    }

    /**
     * @param cluster with which the service can interact.
     */
    public void onTerminate(final Cluster cluster) {
        log.info("Node {} is terminating", nodeId);
    }

    /**
     * @param session   for the client which have been opened.
     * @param timestamp at which the session was opened.
     */
    public void onSessionOpen(final ClientSession session, final long timestamp) {
        log.info("Client session open {} on node {}", session, nodeId);
    }

    /**
     * @param session     that has been closed.
     * @param timestamp   at which the session was closed.
     * @param closeReason the session was closed.
     */
    public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason) {
        log.info("Client session closed {} on node {}, close reason: {}", session, nodeId, closeReason);
        subscriptionService.onSessionClose(session);
        countersSubscriptionService.onSessionClose(session);
    }

    /**
     * @param correlationId for the expired timer.
     * @param timestamp     at which the timer expired.
     */
    public void onTimerEvent(final long correlationId, final long timestamp) {
        cacheTimerService.onTimerEvent(correlationId, timestamp);
        cacheCountersTimerService.onTimerEvent(correlationId, timestamp);
    }


}
