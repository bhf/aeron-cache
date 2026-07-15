package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.results.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * An interface for sending and consuming the results of an AeronCache cluster request via Consumers.
 */
public interface CacheRequestConsumingPublisher<RI extends Reusable, RK extends Reusable, RV extends Reusable, BI, BK, BV> {
    /**
     * Send a message to create a cache instance.
     * Passes the result to the Consumer.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache to create.
     * @param consumer  The consumer of the result.
     */
    void sendCreateCache(String requestId, BI cacheId, Consumer<CreateCacheResult<RI>> consumer);

    /**
     * Send a message to add a cache entry.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     * @param value     The value to use.
     * @param c         The consumer that will handle the result.
     */
    void addCacheEntry(String requestId, BI cacheId, BK key, BV value, long ttl, Consumer<AddCacheEntryResult<RI, RK>> c);

    /**
     * Send a message to get a cache entry.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're getting from.
     * @param key       The key to use.
     * @param c         The consumer to handle the result.
     */
    void getCacheEntry(String requestId, BI cacheId, BK key, Consumer<GetCacheEntryResult<RI, RK, RV>> c);

    /**
     * Send a message to delete a cache.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're deleting.
     */
    void deleteCache(String requestId, BI cacheId, Consumer<DeleteCacheResult<RI>> consumer);

    /**
     * Send a message to remove a cache entry.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're removing an entry from.
     * @param key       The key of the entry we're removing.
     * @param c         The consumer that will handle the result.
     */
    void removeCacheEntry(String requestId, BI cacheId, BK key, Consumer<RemoveCacheEntryResult<RI, RK>> c);

    /**
     * Send a message to clear a cache.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're clearing.
     * @param c         The consumer that will handle the result.
     */
    void clearCache(String requestId, BI cacheId, Consumer<ClearCacheResult<RI>> c);

    /**
     * Send a message to get all cache items.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're getting an entry from.
     * @param c         The consumer that will handle the result.
     */
    void getCacheEntries(String requestId, BI cacheId, Consumer<GetAllCacheEntriesResult<RI, RK, RV>> c);

    /**
     * Send a message to get all cache stats.
     *
     * @param requestId The request ID.
     * @param c         The consumer that will handle the result.
     */
    void getAllCacheStats(String requestId, Consumer<CacheStatsResult<RI>> c);

    /**
     * Send a message to subscribe to cache updates.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache to subscribe too.
     * @param c         The consumer that will handle the result.
     * @param sendSnapshot Whether to send a snapshot for initial state hydration.
     */
    void sendCacheSubscribe(String requestId, List<BI> cacheId, boolean sendSnapshot, Consumer<CacheSubscriptionResult<RI, RK, RV>> c);

    /**
     * Send a message to unsubscribe to cache updates.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache to unsubscribe from.
     * @param c         The consumer that will handle the result.
     */
    void sendCacheUnsubscribe(String requestId, BI cacheId, Consumer<CacheUnsubscribeResult<RI>> c);

    /**
     * Handle requests for bulk operations on the cache.
     *
     * @param requestId The request ID.
     * @param request   The details of the bulk request
     * @param consumer  The consumer that will handle the result.
     */
    void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request, Consumer<BulkCacheOpsResult<RI, RK, RV>> consumer);
}
