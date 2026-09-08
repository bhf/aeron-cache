package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;

import java.util.List;

/**
 * Blocking versions of {@link ClusterMessagePublisher}'s public API.
 */
public interface BlockingClusterRequestPublisher<BI, BK, BV> {
    /**
     * Send a message to create a cache instance, block
     * until you get a response.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache to create.
     */
    void sendCreateCacheBlocking(String requestId, BI cacheId);

    /**
     * Send a message to add a cache entry and block
     * until you get the result back.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     * @param value     The value to use.
     */
    void addCacheEntryBlocking(String requestId, BI cacheId, BK key, BV value, long ttl);

    /**
     * Send a message to get a cache entry synchronously.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     */
    void getCacheEntryBlocking(String requestId, BI cacheId, BK key);

    /**
     * Send a message to clear a cache. Blocks until it gets a response.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're clearing out.
     */
    void clearCacheBlocking(String requestId, BI cacheId);

    /**
     * Send a message to delete a cache in a blocking manner.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're deleting.
     */
    void deleteCacheBlocking(String requestId, BI cacheId);

    /**
     * Send a message to remove a cache entry in a blocking manner.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're removing an entry from.
     * @param key       The key of the entry we're removing.
     */
    void removeCacheEntryBlocking(String requestId, BI cacheId, BK key);

    /**
     * Send a message to cancel a previously scheduled removal of a cache entry in a blocking manner.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache holding the entry.
     * @param key       The key of the entry whose scheduled removal we're cancelling.
     */
    void cancelItemRemovalBlocking(String requestId, BI cacheId, BK key);

    /**
     * Send a message to get all cache entries in a blocking manner.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're removing an entry from.
     */
    void getCacheEntriesBlocking(String requestId, BI cacheId);

    /**
     * Send bulk operations in a blocking manner.
     *
     * @param requestId The Id of this request.
     * @param request The bulk request.
     */
    void sendBulkOperationsBlocking(String requestId, BulkCacheOpsRequest request);

    /**
     * Send a message to get all cache stats from the cluster in a blocking manner.
     *
     * @param requestId The Id of this request.
     */
    void getAllCacheStatsBlocking(String requestId);

    /**
     * Send a request to subscribe to cache updates in a blocking manner.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The cache to subscribe too.
     * @param sendSnapshot Whether to send a snapshot for initial state hydration.
     */
    void sendCacheSubscribeBlocking(String requestId, List<BI> cacheId, boolean sendSnapshot);

    /**
     * Send a request to unsubscribe to cache updates in a blocking manner.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The cache to unsubscribe too.
     */
    void sendCacheUnsubscribeBlocking(String requestId, BI cacheId);
}
