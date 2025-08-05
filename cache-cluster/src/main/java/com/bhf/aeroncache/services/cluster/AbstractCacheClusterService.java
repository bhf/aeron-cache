package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.handlers.NoOpPublicationFailureHandler;
import com.bhf.aeroncache.handlers.PublicationFailureHandler;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cachemanager.CacheManager;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
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
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.Map;
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

    final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final Supplier<I> indexSupplier;
    private Cluster cluster;
    private final CacheTracingService tracingService;
    CacheSubscriptionService<I> subscriptionService;
    private IdleStrategy idleStrategy;
    private final CacheManagerFactory<I, K, V> cacheManagerFactory = new CacheManagerFactory<>();
    private final CacheManager<I, K, V> cacheManager;
    private Consumer<Image> imageConsumer;
    private Consumer<ExclusivePublication> snapshotConsumer;
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

    final CacheSubscriptionResult<I> subscribeResult;
    final CacheUnsubscribeResult<I> unsubscribeResult;
    private final PublicationFailureHandler publicationFailureHandler = new NoOpPublicationFailureHandler();

    protected AbstractCacheClusterService(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier, Supplier<Map<K, V>> mapSupplier, String nodeId, CacheTracingService tracingService) {
        this.createCacheRequestDetails = new CreateCacheRequestDetails<>(indexSupplier.get());
        this.clearCacheRequestDetails = new ClearCacheRequestDetails<>(indexSupplier.get());
        this.removeCacheEntryRequestDetails = new RemoveCacheEntryRequestDetails<>(indexSupplier.get(), keySupplier.get());
        this.addCacheEntryRequestDetails = new AddCacheEntryRequestDetails<>(indexSupplier.get(), keySupplier.get(), valueSupplier.get());
        this.deleteCacheRequestDetails = new DeleteCacheRequestDetails<>(indexSupplier.get());
        this.getCacheEntryRequestDetails = new GetCacheEntryRequestDetails<>(indexSupplier.get(), keySupplier.get());
        this.getAllCacheEntriesRequestDetails = new GetAllCacheEntriesRequestDetails<>(indexSupplier.get());
        this.addEntryFailureResult = new AddCacheEntryResult<>();
        this.getCacheStatsRequestDetails = new GetCacheStatsRequestDetails();
        this.cacheSubscribeRequestDetails = new CacheSubscriptionRequestDetails<>(indexSupplier.get());
        this.cacheUnsubscribeRequestDetails = new CacheUnsubscribeRequestDetails<>(indexSupplier.get());
        this.subscribeResult = new CacheSubscriptionResult<>(indexSupplier.get());
        this.unsubscribeResult = new CacheUnsubscribeResult<>(indexSupplier.get());
        this.cacheManager = cacheManagerFactory.getCacheManager(getSnapshotConsumer(), getImageConsumer(), indexSupplier, keySupplier, valueSupplier, mapSupplier);
        this.nodeId = nodeId;
        this.tracingService = tracingService;
        this.indexSupplier = indexSupplier;
    }

    private Consumer<Image> getImageConsumer() {
        return imageConsumer;
    }

    private Consumer<ExclusivePublication> getSnapshotConsumer() {
        return snapshotConsumer;
    }

    private String nodeId;

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
        headerDecoder.wrap(buffer, offset);
        final int templateId = headerDecoder.templateId();

        switch (templateId) {
            case CreateCacheEncoder.TEMPLATE_ID -> handleCreateCache(session, buffer, offset);
            case AddCacheEntryEncoder.TEMPLATE_ID -> handleAddCacheEntry(session, buffer, offset);
            case GetCacheEntryEncoder.TEMPLATE_ID -> handleGetCacheEntry(session, buffer, offset);
            case RemoveCacheEntryEncoder.TEMPLATE_ID -> handleRemoveCacheEntry(session, buffer, offset);
            case ClearCacheEncoder.TEMPLATE_ID -> handleClearCache(session, buffer, offset);
            case DeleteCacheEncoder.TEMPLATE_ID -> handleDeleteCache(session, buffer, offset);
            case GetAllCacheEntriesEncoder.TEMPLATE_ID -> handleGetAllCacheEntries(session, buffer, offset);
            case GetCacheStatsEncoder.TEMPLATE_ID -> handleGetCacheStats(session, buffer, offset);
            case CacheSubscriptionRequestEncoder.TEMPLATE_ID -> handleCacheSubscriptionRequest(session, buffer, offset);
            case CacheUnsubscribeRequestEncoder.TEMPLATE_ID -> handleCacheUnsubscribeRequest(session, buffer, offset);
            default -> throw new IllegalStateException("Unexpected value: " + templateId);
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
        log.info("Got delete cache request for cache id {}", cacheId);
        var deleteCacheResult = cacheManager.deleteCache(cacheId);
        handlePostDeleteCache(cacheId, deleteCacheResult, requestDetails, session, buffer, offset);
        tracingService.endHandleDeleteCache(requestDetails);
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
        log.info("Got clear cache request for cache id {}", cacheId);
        var clearCacheResult = cacheManager.clearCache(cacheId);
        var requestId = requestDetails.getRequestId();
        clearCacheResult.setRequestId(requestId);
        handlePostClearCache(cacheId, clearCacheResult, session, buffer, offset);
        tracingService.endHandleClearCache(requestDetails);
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
        log.info("Got remove cache entry request for cache id {}, key {}, request Id: {}", cacheId, key, requestId);
        var removeCacheEntryResult = cacheManager.removeCacheEntry(cacheId, key);
        removeCacheEntryResult.setRequestId(requestId);
        handlePostRemoveCacheEntry(cacheId, key, removeCacheEntryResult, session, buffer, offset);
        tracingService.endRemoveCacheEntry(requestDetails);
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
        var requestId = requestDetails.getRequestId();
        log.info("Got add cache entry request for cache id {}, key {}, value {}, request Id: {}", cacheId, key, value, requestId);
        var cache = cacheManager.getCache(cacheId);

        if (cache == null) {
            handleMissingCacheOnAddEntry(session, buffer, offset, cacheId, key, value, requestDetails.getRequestId());
            return;
        }

        var addCacheEntryResult = cache.add(key, value);
        addCacheEntryResult.setRequestId(requestId);
        addCacheEntryResult.getCacheId().copyFrom(cacheId);
        log.info("Result for add entry, key: {}, status: {}, ", addCacheEntryResult.getEntryKey(), addCacheEntryResult.getStatus());
        handlePostAddCacheEntry(cacheId, key, value, addCacheEntryResult, session, buffer, offset);
        tracingService.endAddCacheEntry(requestDetails);
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
        addEntryFailureResult.setStatus(OperationStatus.UNKNOWN_CACHE);
        addEntryFailureResult.setRequestId(requestId);
        addEntryFailureResult.setCacheId(cacheId);
        log.info("Cache {} doesn't exist, tried to add on key key: {}", cacheId, addEntryFailureResult.getEntryKey());
        handlePostAddCacheEntry(cacheId, key, value, addEntryFailureResult, session, buffer, offset);
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
        log.info("Got get cache entry for cache id {} on key {}, requestId {}", cacheId, key, requestId);
        var getCacheEntryResult = cacheManager.getCacheEntry(cacheId, key);
        getCacheEntryResult.setRequestId(requestId);
        getCacheEntryResult.getCacheId().copyFrom(cacheId);
        log.info("Sending GET result: cacheId {}, key {}, value {}, reqId {}, status {}", getCacheEntryResult.getCacheId(), getCacheEntryResult.getEntryKey(), getCacheEntryResult.getEntryValue(), getCacheEntryResult.getRequestId(), getCacheEntryResult.getStatus());
        handlePostGetCacheEntry(cacheId, key, getCacheEntryResult, session, buffer, offset);
        tracingService.endGetCacheEntry(requestDetails);
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
        handlePostGetAllCacheEntries(cacheId, getAllCacheEntriesResult, session, buffer, offset);
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
        log.info("Got create cache request for cache id {}, request Id: {}", cacheId, requestId);
        var cacheCreationResult = cacheManager.createCache(cacheId);
        cacheCreationResult.setRequestId(requestId);
        log.info("Will send result: " + cacheCreationResult.getStatus());
        handlePostCreateCache(cacheId, cacheCreationResult, session, buffer, offset);
        tracingService.endCreateCacheRequest(requestDetails);
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
        handlePostGetCacheStats(cacheStatsResult, session, buffer, offset);
        tracingService.endGetAllStatsRequest(requestDetails);
    }

    void handleCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset) {
        CacheSubscriptionRequestDetails<I> requestDetails = getCacheSubscriptionRequest(session, buffer, offset);
        tracingService.startCacheSubscriptionRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        var cacheId = requestDetails.getCacheId();
        log.info("Got request to subscribe for cache updates on cache: {}, request Id: {}", cacheId, requestId);
        var result = subscriptionService.subscribe(requestDetails, session);
        handlePostCacheSubscriptionRequest(result, session, buffer, offset);
        tracingService.endCacheSubscriptionRequest(requestDetails);
    }

    void handleCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset) {
        CacheUnsubscribeRequestDetails<I> requestDetails = getCacheUnsubscribeRequest(session, buffer, offset);
        tracingService.startCacheUnsubscribeRequest(requestDetails);
        var requestId = requestDetails.getRequestId();
        var cacheId = requestDetails.getCacheId();
        log.info("Got request to unsubscribe for cache updates on cache: {}, request Id: {}", cacheId, requestId);
        var result = subscriptionService.unsubscribe(requestDetails, session);
        handlePostCacheUnsubscribeRequest(result, session, buffer, offset);
        tracingService.endCacheUnsubscribeRequest(requestDetails);
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
     * After the cache is created, send out a CacheCreated message.
     *
     * @param cacheId             The ID of the cache created.
     * @param cacheCreationResult The result from the request to create the cache.
     * @param session             The client session.
     * @param buffer              The buffer from which the creation request was decoded.
     * @param offset              The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostCreateCache(I cacheId, CreateCacheResult<I> cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * After an entry is added to a cache, send out a EntryCreated message.
     *
     * @param cacheId             The ID of the cache in which the entry was created.
     * @param addCacheEntryResult The result from the request to add an entry.
     * @param session             The client session.
     * @param buffer              The buffer from which the entry creation request was created.
     * @param offset              The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostAddCacheEntry(I cacheId, K key, V value, AddCacheEntryResult<I, K> addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Get an entry from the cache, send out a CacheEntry message.
     *
     * @param cacheId             The ID of the cache we need to get the entry from.
     * @param getCacheEntryResult The result from the request to add an entry.
     * @param session             The client session.
     * @param buffer              The buffer from which the entry creation request was created.
     * @param offset              The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostGetCacheEntry(I cacheId, K key, GetCacheEntryResult<I, K, V> getCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Get all entries from the cache.
     *
     * @param cacheId             The ID of the cache we need to get all entries from.
     * @param getCacheEntryResult The result from the request to get all entries.
     * @param session             The client session.
     * @param buffer              The buffer from which the request was created.
     * @param offset              The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostGetAllCacheEntries(I cacheId, GetAllCacheEntriesResult<I, K, V> getCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * After an entry is removed from the cache, send out a EntryRemoved message.
     *
     * @param cacheId                The ID of the cache in which the entry was removed.
     * @param removeCacheEntryResult The result from the request to remove an entry.
     * @param session                The client session.
     * @param buffer                 The buffer from which the entry removal request was created.
     * @param offset                 The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostRemoveCacheEntry(I cacheId, K key, RemoveCacheEntryResult<I, K> removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * After a cache is cleared, send out a CacheCleared message.
     *
     * @param cacheId          The ID of the cache in which the entry was removed.
     * @param clearCacheResult The result from the request to clear a cache.
     * @param session          The client session.
     * @param buffer           The buffer from which the clear request was created.
     * @param offset           The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostClearCache(I cacheId, ClearCacheResult<I> clearCacheResult, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * After a cache is deleted, send out a CacheDeleted message.
     *
     * @param cacheId           The ID of the cache which was deleted.
     * @param deleteCacheResult The result of deleting the cache.
     * @param requestDetails    The original request to delete the cache.
     * @param session           The client session.
     * @param buffer            The buffer from which the delete request was created.
     * @param offset            The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostDeleteCache(I cacheId, DeleteCacheResult<I> deleteCacheResult, DeleteCacheRequestDetails<I> requestDetails, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Send out the cache stats.
     *
     * @param cacheStatsResult The stats across all caches.
     * @param session          The client session.
     * @param buffer           The buffer from which the delete request was created.
     * @param offset           The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostGetCacheStats(CacheStatsResult<I> cacheStatsResult, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Send out the result of subscribing to a cache.
     *
     * @param subscriptionRequestResult The result of subscribing.
     * @param session                   The client session.
     * @param buffer                    The buffer from which the delete request was created.
     * @param offset                    The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<I> subscriptionRequestResult, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * Send out the result of unsubscribing to a cache.
     *
     * @param unsubscribeResponse The result of unsubscribing.
     * @param session             The client session.
     * @param buffer              The buffer from which the delete request was created.
     * @param offset              The offset from within the buffer to decode the original request from.
     */
    protected abstract void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<I> unsubscribeResponse, ClientSession session, DirectBuffer buffer, int offset);

    /**
     * @param session   Session to send the message too.
     * @param msgBuffer The buffer containing the message.
     * @param len       The length of the message.
     */
    void sendMessage(final ClientSession session, MutableDirectBuffer msgBuffer, int len) {
        long offered = 0;
        while ((offered = session.offer(msgBuffer, 0, len)) < 0) {
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
    }


}
