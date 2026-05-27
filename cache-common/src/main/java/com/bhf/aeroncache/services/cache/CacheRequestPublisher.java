package com.bhf.aeroncache.services.cache;


import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;

/**
 * Requests that an Aeron Cache will handle.
 */
public interface CacheRequestPublisher {
    /**
     * Send a message to create a cache instance.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache to create.
     */
    void sendCreateCache(String requestId, String cacheId);

    /**
     * Send a message to add a cache entry in a non-blocking manner.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     * @param value     The value to use.
     */
    void addCacheEntry(String requestId, String cacheId, String key, String value, long ttl);

    /**
     * Send a message to get a cache entry.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     */
    void getCacheEntry(String requestId, String cacheId, String key);

    /**
     * Send a message to clear a cache.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're clearing out.
     */
    void clearCache(String requestId, String cacheId);

    /**
     * Send a message to delete a cache.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're deleting.
     */
    void deleteCache(String requestId, String cacheId);

    /**
     * Send a message to remove a cache entry.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're removing an entry from.
     * @param key       The key of the entry we're removing.
     */
    void removeCacheEntry(String requestId, String cacheId, String key);

    /**
     * Send a message to get all cache entries.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're removing an entry from.
     */
    void getCacheEntries(String requestId, String cacheId);

    /**
     * Send a message to get all cache stats from the cluster.
     *
     * @param requestId The Id of this request.
     */
    void getAllCacheStats(String requestId);

    /**
     * Send a request to subscribe to cache updates.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The cache to subscribe too.
     */
    void sendCacheSubscribe(String requestId, String cacheId);

    /**
     * Send a request to unsubscribe to cache updates.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The cache to unsubscribe too.
     */
    void sendCacheUnsubscribe(String requestId, String cacheId);

    /**
     * Send a bulk operation request.
     *
     * @param requestId The Id of this request.
     * @param request   The bulk request.
     */
    void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request);
}
