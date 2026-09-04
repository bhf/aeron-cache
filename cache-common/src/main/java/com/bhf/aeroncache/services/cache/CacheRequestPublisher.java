package com.bhf.aeroncache.services.cache;


import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;

import java.util.List;

/**
 * Requests that an Aeron Cache will handle.
 * @param <BI> Base index type.
 * @param <BK>  Base key type.
 * @param <BV>  Base value type.
 */
public interface CacheRequestPublisher<BI,BK,BV> {

    /**
     * Send a message to create a cache instance.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache to create.
     */
    void sendCreateCache(String requestId, BI cacheId);

    /**
     * Send a message to add a cache entry in a non-blocking manner.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     * @param value     The value to use.
     */
    void addCacheEntry(String requestId, BI cacheId, BK key, BV value, long ttl);

    /**
     * Send a message to get a cache entry.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     */
    void getCacheEntry(String requestId, BI cacheId, BK key);

    /**
     * Send a message to clear a cache.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're clearing out.
     */
    void clearCache(String requestId, BI cacheId);

    /**
     * Send a message to delete a cache.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're deleting.
     */
    void deleteCache(String requestId, BI cacheId);

    /**
     * Send a message to remove a cache entry.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're removing an entry from.
     * @param key       The key of the entry we're removing.
     */
    void removeCacheEntry(String requestId, BI cacheId, BK key);

    /**List<String>
     * Send a message to get all cache entries.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the cache we're removing an entry from.
     */
    void getCacheEntries(String requestId, BI cacheId);

    /**
     * Send a message to get all cache stats from the cluster.
     *
     * @param requestId The Id of this request.
     */
    void getAllCacheStats(String requestId);

    /**
     * Send a request to subscribe to cache updates.
     *
     * @param requestId    The Id of this request.
     * @param cacheId      The cache to subscribe too.
     * @param sendSnapshot Whether to return a snapshot of the cache for initial hydration.
     */
    void sendCacheSubscribe(String requestId, List<BI> cacheId, boolean sendSnapshot);

    /**
     * Send a request to unsubscribe to cache updates.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The cache to unsubscribe too.
     */
    void sendCacheUnsubscribe(String requestId, BI cacheId);

    /**
     * Send a bulk operation request.
     *
     * @param requestId The Id of this request.
     * @param request   The bulk request.
     */
    void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request);

    /**
     * Send a message to increment a counter by a given amount.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the counter cache.
     * @param key       The key of the counter to increment.
     * @param amount    The amount to increment the counter by.
     * @param ttl       The time to live for the counter entry.
     */
    default void incrementCounter(String requestId, BI cacheId, BK key, long amount, long ttl) {
        throw new UnsupportedOperationException("Increment counter is only supported for counter caches");
    }

    /**
     * Send a message to decrement a counter by a given amount.
     *
     * @param requestId The Id of this request.
     * @param cacheId   The ID of the counter cache.
     * @param key       The key of the counter to decrement.
     * @param amount    The amount to decrement the counter by.
     * @param ttl       The time to live for the counter entry.
     */
    default void decrementCounter(String requestId, BI cacheId, BK key, long amount, long ttl) {
        throw new UnsupportedOperationException("Decrement counter is only supported for counter caches");
    }
}
