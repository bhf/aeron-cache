package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.handlers.NoOpPublicationFailureHandler;
import com.bhf.aeroncache.handlers.PublicationFailureHandler;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.Cache;
import com.bhf.aeroncache.services.cachemanager.CacheManager;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.cachemanager.CacheSchemaDetailsProvider;
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
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The cache cluster service provides access to a CacheManager via an
 * Aeron cluster interface. It processes the core messages of the cache and
 * delegates those to the implementation of the
 * {@link CacheManager}. This level of abstraction is not responsible for
 * decoding of the actual messages.
 */
@Log4j2
public abstract class AbstractCacheClusterService<I extends Reusable, K extends Reusable, V extends Reusable> implements ClusteredService {

    private final Supplier<I> indexSupplier;
    private final CacheSchemaDetailsProvider schemaDetails;
    private Cluster cluster;
    private final CacheTracingService tracingService;
    CacheSubscriptionService<I, K, V> subscriptionService;
    private IdleStrategy idleStrategy;
    private final CacheManagerFactory<I, K, V> cacheManagerFactory;
    private final CacheManager<I, K, V> cacheManager;
    final CreateCacheRequestDetails<I> createCacheRequestDetails;
    final ClearCacheRequestDetails<I> clearCacheRequestDetails;
    final RemoveCacheEntryRequestDetails<I, K> removeCacheEntryRequestDetails;
    final AddCacheEntryRequestDetails<I, K, V> addCacheEntryRequestDetails;
    final DeleteCacheRequestDetails<I> deleteCacheRequestDetails;
    final GetCacheEntryRequestDetails<I, K> getCacheEntryRequestDetails;
    final GetAllCacheEntriesRequestDetails<I> getAllCacheEntriesRequestDetails;
    final AddCacheEntryResult<I, K> addEntryFailureResult;
    final GetCacheStatsRequestDetails getCacheStatsRequestDetails;
    final CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails;
    final CacheUnsubscribeRequestDetails<I> cacheUnsubscribeRequestDetails;
    final BulkCacheOpsRequestDetails<I,K,V> bulkCacheOpsRequestDetails;
    final BulkCacheOpsResult<I, K, V> bulkOpsResult;

    final CacheSubscriptionResult<I> subscribeResult;
    final CacheUnsubscribeResult<I> unsubscribeResult;
    private final PublicationFailureHandler publicationFailureHandler = new NoOpPublicationFailureHandler();

    private String nodeId;
    private long timerCorrelationId = 0;
    private final Long2ObjectHashMap<Consumer> timerCallbacks = new Long2ObjectHashMap();
    @Setter
    @Getter
    private boolean dynamicCacheCreationEnabled = false;

    protected AbstractCacheClusterService(String nodeId, CacheTracingService tracingService, CacheManagerFactory<I, K, V> cacheManagerFactory) {
        this.createCacheRequestDetails = new CreateCacheRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.clearCacheRequestDetails = new ClearCacheRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.removeCacheEntryRequestDetails = new RemoveCacheEntryRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.addCacheEntryRequestDetails = new AddCacheEntryRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get(), cacheManagerFactory.getValueSupplier().get());
        this.deleteCacheRequestDetails = new DeleteCacheRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.getCacheEntryRequestDetails = new GetCacheEntryRequestDetails<>(cacheManagerFactory.getIndexSupplier().get(), cacheManagerFactory.getKeySupplier().get());
        this.getAllCacheEntriesRequestDetails = new GetAllCacheEntriesRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.addEntryFailureResult = new AddCacheEntryResult<>();
        this.getCacheStatsRequestDetails = new GetCacheStatsRequestDetails();
        this.cacheSubscribeRequestDetails = new CacheSubscriptionRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.cacheUnsubscribeRequestDetails = new CacheUnsubscribeRequestDetails<>(cacheManagerFactory.getIndexSupplier().get());
        this.bulkCacheOpsRequestDetails = new BulkCacheOpsRequestDetails<>();
        this.subscribeResult = new CacheSubscriptionResult<>(cacheManagerFactory.getIndexSupplier().get());
        this.unsubscribeResult = new CacheUnsubscribeResult<>(cacheManagerFactory.getIndexSupplier().get());
        this.cacheManagerFactory = cacheManagerFactory;
        this.cacheManager = cacheManagerFactory.getCacheManager();
        this.nodeId = nodeId;
        this.tracingService = tracingService;
        this.indexSupplier = cacheManagerFactory.getIndexSupplier();
        this.schemaDetails = cacheManagerFactory.getSchemaDetailsProvider();
        this.bulkOpsResult = new BulkCacheOpsResult<>(cacheManagerFactory.getIndexSupplier(),
                cacheManagerFactory.getKeySupplier(), cacheManagerFactory.getValueSupplier());
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
            handleCreateCache(session, buffer, offset);
        } else if (templateId == schemaDetails.getAddCacheEntryId()) {
            handleAddCacheEntry(session, buffer, offset);
        } else if (templateId == schemaDetails.getGetCacheEntryId()) {
            handleGetCacheEntry(session, buffer, offset);
        } else if (templateId == schemaDetails.getRemoveCacheEntryId()) {
            handleRemoveCacheEntry(session, buffer, offset);
        } else if (templateId == schemaDetails.getClearCacheId()) {
            handleClearCache(session, buffer, offset);
        } else if (templateId == schemaDetails.getDeleteCacheId()) {
            handleDeleteCache(session, buffer, offset);
        } else if (templateId == schemaDetails.getGetAllCacheEntriesId()) {
            handleGetAllCacheEntries(session, buffer, offset);
        } else if (templateId == schemaDetails.getGetCacheStatsId()) {
            handleGetCacheStats(session, buffer, offset);
        } else if (templateId == schemaDetails.getCacheSubscriptionRequestId()) {
            handleCacheSubscriptionRequest(session, buffer, offset);
        } else if (templateId == schemaDetails.getCacheUnsubscribeRequestId()) {
            handleCacheUnsubscribeRequest(session, buffer, offset);
        } else if (templateId == schemaDetails.getBulkCacheOpsRequestId()) {
            handleBulkOpsRequest(session, buffer, offset);
        } else {
            throw new IllegalStateException("Unexpected value: " + templateId);
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
        this.idleStrategy = cluster.idleStrategy();
        if (null != snapshotImage) {
            loadSnapshot(cluster, snapshotImage);
        }

        log.info("Starting subscription service");
        this.subscriptionService = new CacheSubscriptionServiceImpl<>(idleStrategy, subscribeResult, unsubscribeResult, indexSupplier);
    }

    /**
     * Handle a delete cache message.
     *
     * @param session Session requesting the delete operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleDeleteCache(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails = getDeleteCacheRequestDetails(session, buffer, offset);
        tracingService.startHandleDeleteCache(requestDetails);
        I cacheId = requestDetails.getCacheId();
        log.info("DELETE CACHE ID ON REQUEST {}", cacheId);
        var deleteCacheResult = processDeleteCache(cacheId, requestDetails.getRequestId());
        log.info("DELETE RESULT CACHE ID {}", deleteCacheResult.getCacheId());
        handlePostDeleteCache(cacheId, deleteCacheResult, session);
        tracingService.endHandleDeleteCache(requestDetails);
    }

    private DeleteCacheResult<I> processDeleteCache(I cacheId, String requestId) {
        log.info("Got delete cache request for cache id {}, request id {}", cacheId, requestId);
        var res = cacheManager.deleteCache(cacheId);
        res.setRequestId(requestId);
        return res;
    }

    /**
     * Handle a cache clear request.
     *
     * @param session Session requesting the clear operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleClearCache(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails = getClearCacheRequestDetails(session, buffer, offset);
        tracingService.startHandleClearCache(requestDetails);
        I cacheId = requestDetails.getCacheId();
        var requestId = requestDetails.getRequestId();
        var clearCacheResult = processClearCache(cacheId, requestId);
        handlePostClearCache(cacheId, clearCacheResult, session);
        tracingService.endHandleClearCache(requestDetails);
    }

    private ClearCacheResult<I> processClearCache(I cacheId, String requestId) {
        log.info("Got clear cache request for cache id {} with requestId {}", cacheId, requestId);
        var clearCacheResult = cacheManager.clearCache(cacheId);
        clearCacheResult.setRequestId(requestId);
        return clearCacheResult;
    }

    /**
     * Handle a request to remove a cache entry.
     *
     * @param session Session requesting the remove cache operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleRemoveCacheEntry(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails = getRemoveCacheEntryRequestDetails(session, buffer, offset);
        tracingService.startRemoveCacheEntry(requestDetails);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        var requestId = requestDetails.getRequestId();
        var removeCacheEntryResult = processRemoveCacheEntry(cacheId, key, requestId);
        handlePostRemoveCacheEntry(cacheId, key, removeCacheEntryResult, session);
        tracingService.endRemoveCacheEntry(requestDetails);
    }

    private RemoveCacheEntryResult<I, K> processRemoveCacheEntry(I cacheId, K key, String requestId) {
        log.info("Got remove cache entry request for cache id {}, key {}, request Id: {}", cacheId, key, requestId);
        var removeCacheEntryResult = cacheManager.removeCacheEntry(cacheId, key);
        removeCacheEntryResult.setRequestId(requestId);
        return removeCacheEntryResult;
    }

    /**
     * Handle a request to add an entry to a cache.
     *
     * @param session Session requesting the add entry operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleAddCacheEntry(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails = getAddCacheEntryRequestDetails(session, buffer, offset);
        tracingService.startAddCacheEntry(requestDetails);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        V value = requestDetails.getValue();
        long ttl = requestDetails.getTtl();
        var requestId = requestDetails.getRequestId();
        var addCacheEntryResult = processAddCacheEntry(cacheId, key, value, ttl, requestId, session, buffer, offset);

        log.info("Result for add entry, key: {}, status: {}, ", addCacheEntryResult.getEntryKey(), addCacheEntryResult.getStatus());
        handlePostAddCacheEntry(requestDetails.getCacheId(), requestDetails.getKey(), requestDetails.getValue(), addCacheEntryResult, session);
        tracingService.endAddCacheEntry(requestDetails);
    }

    private AddCacheEntryResult<I,K> processAddCacheEntry(I cacheId, K key, V value, long ttl, String requestId, ClientSession session, DirectBuffer buffer, int offset) {
        log.info("Got add cache entry request for cache id {}, key {}, value {}, ttl {}, request Id: {}", cacheId, key, value, ttl, requestId);
        var cache = cacheManager.getCache(cacheId);

        if (cache == null) {
            if (dynamicCacheCreationEnabled) {
                var createCacheResult = cacheManager.createCache(cacheId);
                log.info("Created cache {} dynamically, result {}", cacheId, createCacheResult);
                cache = cacheManager.getCache(cacheId);
            } else if(session!=null){
                handleMissingCacheOnAddEntry(session, buffer, offset, cacheId, key, value, requestId);
                return addEntryFailureResult;
            }
        }

        var addCacheEntryResult = cache.add(key, value);
        addCacheEntryResult.setRequestId(requestId);
        addCacheEntryResult.getCacheId().copyFrom(cacheId);

        if (ttl > 0 && addCacheEntryResult.getStatus() == CacheOperationStatus.SUCCESS) {
            long now = cluster.time();
            long deadline = now + ttl;
            scheduleItemRemoval(cacheId, key, cache, deadline);
        }

        return addCacheEntryResult;
    }


    /**
     * Schedule the removal of an item from a cache.
     *
     * @param cacheId  The ID of the cache to remove from.
     * @param key      The key to remove.
     * @param cache    The cache to remove from.
     * @param deadline The epoch time at which to remove the item.
     */
    private void scheduleItemRemoval(I cacheId, K key, Cache<I, K, V> cache, long deadline) {
        timerCorrelationId++;
        cluster.scheduleTimer(timerCorrelationId, deadline);
        log.info("Scheduled timer for {} to remove key {} from cache {} correlationId {}", deadline, key, cacheId, timerCorrelationId);

        final var keyToRemove = cacheManagerFactory.getKeySupplier().get();
        keyToRemove.copyFrom(key);

        final var cacheToRemoveOn = cacheManagerFactory.getIndexSupplier().get();
        cacheToRemoveOn.copyFrom(cacheId);

        timerCallbacks.put(timerCorrelationId, o -> {
            try {
                var removeItemResult = cache.remove(keyToRemove);
                removeItemResult.getCacheId().copyFrom(cacheToRemoveOn);
                log.info("Removed {} from cache {} on timer, result: {}", keyToRemove, cacheToRemoveOn, removeItemResult);
                handlePostRemoveTimerCacheEntry(cacheToRemoveOn, keyToRemove, removeItemResult);
            } catch (Exception e) {
                log.error("Error processing timer to remove {} from cache {}", keyToRemove, cacheToRemoveOn, e);
            }
        });
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
    private void handleMissingCacheOnAddEntry(ClientSession session, DirectBuffer buffer, int offset, I cacheId, K key, V value, String requestId) {
        addEntryFailureResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        addEntryFailureResult.setRequestId(requestId);
        addEntryFailureResult.setCacheId(cacheId);
        log.info("Cache {} doesn't exist, tried to add on key key: {}", cacheId, addEntryFailureResult.getEntryKey());
        handlePostAddCacheEntry(cacheId, key, value, addEntryFailureResult, session);
    }

    /**
     * Handle a request to get a cache entry.
     *
     * @param session Session requesting the add entry operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleGetCacheEntry(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails = getCacheEntryRequestDetails(session, buffer, offset);
        tracingService.startGetCacheEntry(requestDetails);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        var requestId = requestDetails.getRequestId();
        var getCacheEntryResult = processGetCacheEntry(cacheId, key, requestId);
        log.info("Sending GET result: cacheId {}, key {}, value {}, reqId {}, status {}", getCacheEntryResult.getCacheId(), getCacheEntryResult.getEntryKey(), getCacheEntryResult.getEntryValue(), getCacheEntryResult.getRequestId(), getCacheEntryResult.getStatus());
        handlePostGetCacheEntry(cacheId, key, getCacheEntryResult, session);
        tracingService.endGetCacheEntry(requestDetails);
    }

    private GetCacheEntryResult<I, K, V> processGetCacheEntry(I cacheId, K key, String requestId) {
        log.info("Got get cache entry for cache id {} on key {}, requestId {}", cacheId, key, requestId);
        var getCacheEntryResult = cacheManager.getCacheEntry(cacheId, key);
        getCacheEntryResult.setRequestId(requestId);
        getCacheEntryResult.getCacheId().copyFrom(cacheId);
        return getCacheEntryResult;
    }

    /**
     * Handle a request to get all items from a cache.
     *
     * @param session Session requesting the add entry operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    private void handleGetAllCacheEntries(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails = getAllCacheEntriesRequestDetails(session, buffer, offset);
        tracingService.startGetAllCacheEntries(requestDetails);
        I cacheId = requestDetails.getCacheId();
        var requestId = requestDetails.getRequestId();
        log.info("Got get cache content for cache id {}, requestId {}", cacheId, requestId);

        var getAllCacheEntriesResult = cacheManager.getAllCacheEntries(cacheId);
        getAllCacheEntriesResult.setRequestId(requestId);
        getAllCacheEntriesResult.getCacheId().copyFrom(cacheId);
        handlePostGetAllCacheEntries(cacheId, getAllCacheEntriesResult, session);
        tracingService.endGetAllCacheEntries(requestDetails);
    }

    /**
     * Handle a request to create a cache.
     *
     * @param session Session requesting the create cache operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleCreateCache(ClientSession session, DirectBuffer buffer, int offset) {
        CreateCacheRequestDetails<I> requestDetails = getCreateCacheRequestDetails(session, buffer, offset);
        tracingService.startCreateCacheRequest(requestDetails);
        I cacheId = requestDetails.getCacheId();
        var requestId = requestDetails.getRequestId();
        var cacheCreationResult = processCreateCache(cacheId, requestId);
        log.info("Will send result: " + cacheCreationResult.getStatus());
        handlePostCreateCache(cacheId, cacheCreationResult, session);
        tracingService.endCreateCacheRequest(requestDetails);
    }

    private CreateCacheResult<I> processCreateCache(I cacheId, String requestId) {
        log.info("Got create cache request for cache id {}, request Id: {}", cacheId, requestId);
        var cacheCreationResult = cacheManager.createCache(cacheId);
        cacheCreationResult.setRequestId(requestId);
        return cacheCreationResult;
    }

    /**
     * Handle a request to get all cache stats.
     *
     * @param session Session requesting the get all stats operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleGetCacheStats(ClientSession session, DirectBuffer buffer, int offset) {
        GetCacheStatsRequestDetails requestDetails = getCacheStatsRequestDetails(session, buffer, offset);
        tracingService.startGetAllStatsRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        log.info("Got request for all cache stats, request Id: {}", requestId);
        var cacheStatsResult = cacheManager.getCacheStatsResult();
        cacheStatsResult.setRequestId(requestId);
        handlePostGetCacheStats(cacheStatsResult, session);
        tracingService.endGetAllStatsRequest(requestDetails);
    }

    void handleCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset) {
        CacheSubscriptionRequestDetails<I> requestDetails = getCacheSubscriptionRequest(session, buffer, offset);
        tracingService.startCacheSubscriptionRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        var cacheId = requestDetails.getCacheId();
        log.info("Got request to subscribe for cache updates on cache: {}, request Id: {}", cacheId, requestId);
        var result = subscriptionService.subscribe(requestDetails, session);
        handlePostCacheSubscriptionRequest(result, session);
        tracingService.endCacheSubscriptionRequest(requestDetails);
    }

    void handleCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset) {
        CacheUnsubscribeRequestDetails<I> requestDetails = getCacheUnsubscribeRequest(session, buffer, offset);
        tracingService.startCacheUnsubscribeRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        var cacheId = requestDetails.getCacheId();
        log.info("Got request to unsubscribe for cache updates on cache: {}, request Id: {}", cacheId, requestId);
        var result = subscriptionService.unsubscribe(requestDetails, session);
        handlePostCacheUnsubscribeRequest(result, session);
        tracingService.endCacheUnsubscribeRequest(requestDetails);
    }

    void handleBulkOpsRequest(ClientSession session, DirectBuffer buffer, int offset) {
        BulkCacheOpsRequestDetails<I,K,V> requestDetails = getBulkOpsRequest(session, buffer, offset);
        tracingService.startBulkOpsRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        log.info("Got bulk operations request with Id: {}", requestId);
        BulkCacheOpsResult<I,K,V> res = processBulkOperations(requestDetails);
        handlePostBulkOpsRequest(res, session);
        tracingService.endBulkOpsRequest(requestDetails);
    }

    private BulkCacheOpsResult<I,K,V> processBulkOperations(BulkCacheOpsRequestDetails<I,K,V> requestDetails) {

        bulkOpsResult.clear();

        for (var op : requestDetails.getOperations()) {
            switch (op.getOperationType()) {
                case CREATE_CACHE -> handleBulkOpCreateCache(op, bulkOpsResult);
                case ADD_ITEM -> handleBulkOpAddItem(op, bulkOpsResult);
                case CLEAR_CACHE -> handleBulkOpClearCache(op, bulkOpsResult);
                case GET_ITEM -> handleBulkOpGetItem(op, bulkOpsResult);
                case DELETE_CACHE -> handleBulkOpDeleteCache(op, bulkOpsResult);
                case REMOVE_ITEM -> handleBulkOpRemoveItem(op, bulkOpsResult);
            }
        }

        return bulkOpsResult;
    }

    private void handleBulkOpRemoveItem(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult) {
        I cacheId = op.getCacheId();
        var requestId = op.getRequestId();
        K key = op.getKey();
        var result = processRemoveCacheEntry(cacheId, key, requestId);
        log.debug("Bulk request, remove item result: {}", result);
        bulkOpsResult.addResult(result);
    }

    private void handleBulkOpDeleteCache(CacheOperationRequestDetails<I,K,V> op, BulkCacheOpsResult<I,K,V> bulkOpsResult) {
        I cacheId = op.getCacheId();
        var result = processDeleteCache(cacheId, op.getRequestId());
        log.debug("Bulk request, delete cache result: {}", result);
        bulkOpsResult.addResult(result);

        // update the subscription service
        handlePostDeleteCache(cacheId, result, null);
    }

    private void handleBulkOpGetItem(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult) {
        I cacheId = op.getCacheId();
        var requestId = op.getRequestId();
        K key = op.getKey();
        var result = processGetCacheEntry(cacheId, key, requestId);
        log.debug("Bulk request, get item result: {}", result);
        bulkOpsResult.addResult(result);
    }

    private void handleBulkOpClearCache(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult) {
        I cacheId = op.getCacheId();
        var requestId = op.getRequestId();
        var result = processClearCache(cacheId, requestId);
        log.debug("Bulk request, clear cache result: {}", result);
        bulkOpsResult.addResult(result);

        // update the subscription service
        handlePostClearCache(cacheId, result, null);
    }

    private void handleBulkOpAddItem(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult) {
        I cacheId = op.getCacheId();
        K key = op.getKey();
        V value = op.getValue();
        var ttl = op.getTtl();
        var requestId = op.getRequestId();
        var result = processAddCacheEntry(cacheId, key, value, ttl, requestId, null, null, 0);
        log.debug("Bulk request, add item result: {}", result);
        bulkOpsResult.addResult(result);

        // update the subscription service
        handlePostAddCacheEntry(cacheId, key, value, result, null);
    }

    private void handleBulkOpCreateCache(CacheOperationRequestDetails<I, K, V> op, BulkCacheOpsResult<I, K, V> bulkOpsResult) {
        I cacheId = op.getCacheId();
        var requestId = op.getRequestId();
        var result = processCreateCache(cacheId, requestId);
        log.debug("Bulk request, create cache result: {}", result);
        bulkOpsResult.addResult(result);
    }

    /**
     * Decode the CreateCache message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The CreateCacheRequestDetails flyweight.
     */
    protected abstract CreateCacheRequestDetails<I> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the ClearCache message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The ClearCacheRequestDetails flyweight.
     */
    protected abstract ClearCacheRequestDetails<I> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the RemoveCacheEntry message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The RemoveCacheEntryRequestDetails flyweight.
     */
    protected abstract RemoveCacheEntryRequestDetails<I, K> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the AddCacheEntry message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The AddCacheEntryRequestDetails flyweight.
     */
    protected abstract AddCacheEntryRequestDetails<I, K, V> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the GetCacheEntry message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The GetCacheEntryRequestDetails flyweight.
     */
    protected abstract GetCacheEntryRequestDetails<I, K> getCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the GetAllCacheEntries message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The GetAllCacheEntriesRequestDetails flyweight.
     */
    protected abstract GetAllCacheEntriesRequestDetails<I> getAllCacheEntriesRequestDetails(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the DeleteCache message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The DeleteCacheRequestDetails flyweight.
     */
    protected abstract DeleteCacheRequestDetails<I> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the CacheStatsRequest message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The GetCacheStatsRequestDetails flyweight.
     */
    protected abstract GetCacheStatsRequestDetails getCacheStatsRequestDetails(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the CacheSubscriptionRequest message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The CacheSubscriptionRequestDetails flyweight.
     */
    protected abstract CacheSubscriptionRequestDetails<I> getCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Decode the CacheUnsubscribeRequest message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The CacheUnsubscribeRequestDetails flyweight.
     */
    protected abstract CacheUnsubscribeRequestDetails<I> getCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset);


    /**
     * Decode the BulkCacheOpsRequestDetails message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The BulkCacheOpsRequestDetails flyweight.
     */
    protected abstract BulkCacheOpsRequestDetails<I,K,V> getBulkOpsRequest(ClientSession session, DirectBuffer buffer, int offset);

    /**
     * After the cache is created, send out a CacheCreated message.
     *
     * @param cacheId             The ID of the cache created.
     * @param cacheCreationResult The result from the request to create the cache.
     * @param session             The client session.
     */
    protected abstract void handlePostCreateCache(I cacheId, CreateCacheResult<I> cacheCreationResult, ClientSession session);

    /**
     * After an entry is added to a cache, send out a EntryCreated message.
     *
     * @param cacheId             The ID of the cache in which the entry was created.
     * @param addCacheEntryResult The result from the request to add an entry.
     * @param session             The client session.
     */
    protected abstract void handlePostAddCacheEntry(I cacheId, K key, V value, AddCacheEntryResult<I, K> addCacheEntryResult, ClientSession session);

    /**
     * Get an entry from the cache, send out a CacheEntry message.
     *
     * @param cacheId             The ID of the cache we need to get the entry from.
     * @param getCacheEntryResult The result from the request to add an entry.
     * @param session             The client session.
     */
    protected abstract void handlePostGetCacheEntry(I cacheId, K key, GetCacheEntryResult<I, K, V> getCacheEntryResult, ClientSession session);

    /**
     * Get all entries from the cache.
     *
     * @param cacheId             The ID of the cache we need to get all entries from.
     * @param getCacheEntryResult The result from the request to get all entries.
     * @param session             The client session.
     */
    protected abstract void handlePostGetAllCacheEntries(I cacheId, GetAllCacheEntriesResult<I, K, V> getCacheEntryResult, ClientSession session);

    /**
     * After an entry is removed from the cache, send out a EntryRemoved message.
     *
     * @param cacheId                The ID of the cache in which the entry was removed.
     * @param removeCacheEntryResult The result from the request to remove an entry.
     * @param session                The client session.
     */
    protected abstract void handlePostRemoveCacheEntry(I cacheId, K key, RemoveCacheEntryResult<I, K> removeCacheEntryResult, ClientSession session);

    /**
     * After an entry is removed from the cache on a timer event.
     *
     * @param cacheId                The ID of the cache in which the entry was removed.
     * @param key
     * @param removeCacheEntryResult The result from the request to remove an entry.
     */
    protected abstract void handlePostRemoveTimerCacheEntry(I cacheId, K key, RemoveCacheEntryResult<I, K> removeCacheEntryResult);

    /**
     * After a cache is cleared, send out a CacheCleared message.
     *
     * @param cacheId          The ID of the cache in which the entry was removed.
     * @param clearCacheResult The result from the request to clear a cache.
     * @param session          The client session.
     */
    protected abstract void handlePostClearCache(I cacheId, ClearCacheResult<I> clearCacheResult, ClientSession session);

    /**
     * After a cache is deleted, send out a CacheDeleted message.
     *
     * @param cacheId           The ID of the cache which was deleted.
     * @param deleteCacheResult The result of deleting the cache.
     * @param session           The client session.
     */
    protected abstract void handlePostDeleteCache(I cacheId, DeleteCacheResult<I> deleteCacheResult, ClientSession session);

    /**
     * Send out the cache stats.
     *
     * @param cacheStatsResult The stats across all caches.
     * @param session          The client session.
     */
    protected abstract void handlePostGetCacheStats(CacheStatsResult<I> cacheStatsResult, ClientSession session);

    /**
     * Send out the result of subscribing to a cache.
     *
     * @param subscriptionRequestResult The result of subscribing.
     * @param session                   The client session.
     */
    protected abstract void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<I> subscriptionRequestResult, ClientSession session);

    /**
     * Send out the result of unsubscribing to a cache.
     *
     * @param unsubscribeResponse The result of unsubscribing.
     * @param session             The client session.
     */
    protected abstract void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<I> unsubscribeResponse, ClientSession session);

    /**
     * Send out the result of bulk operations done on the cache.
     *
     * @param bulkCacheOpsResult The result of the bulk operation.
     * @param session            The client session.
     */
    protected abstract void handlePostBulkOpsRequest(BulkCacheOpsResult<I,K,V> bulkCacheOpsResult, ClientSession session);


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
        log.info("Got request to take snapshot");
        cacheManager.takeSnapshot(snapshotPublication);
    }

    /**
     * Load the state from an Image.
     *
     * @param cluster       The cluster from which we are loading the snapshot.
     * @param snapshotImage The snapshot image.
     */
    private void loadSnapshot(final Cluster cluster, final Image snapshotImage) {
        log.info("Got request to load snapshot");
        cacheManager.loadSnapshot(snapshotImage);
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
    }

    /**
     * @param correlationId for the expired timer.
     * @param timestamp     at which the timer expired.
     */
    public void onTimerEvent(final long correlationId, final long timestamp) {
        var timerConsumer = timerCallbacks.remove(correlationId);

        if (timerConsumer != null) {
            log.info("Firing timer on correlation Id {}", correlationId);
            timerConsumer.accept(timestamp);
        }
    }


}
