package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;

import java.util.function.Consumer;

/**
 * An interface for sending and consuming the results of an AeronCache cluster request via Consumers.
 */
public interface CacheRequestConsumingPublisher<I extends Reusable, K extends Reusable, V extends Reusable> {
    /**
     * Send a message to create a cache instance.
     * Passes the result to the Consumer.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache to create.
     * @param consumer  The consumer of the result.
     */
    void sendCreateCache(String requestId, String cacheId, Consumer<CreateCacheResult<I>> consumer);

    /**
     * Send a message to add a cache entry.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     * @param value     The value to use.
     * @param c         The consumer that will handle the result.
     */
    void addCacheEntry(String requestId, String cacheId, String key, String value, Consumer<AddCacheEntryResult<I, K>> c);

    /**
     * Send a message to get a cache entry.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're getting from.
     * @param key       The key to use.
     * @param c         The consumer to handle the result.
     */
    void getCacheEntry(String requestId, String cacheId, String key, Consumer<GetCacheEntryResult<I, K, V>> c);

    /**
     * Send a message to delete a cache.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're deleting.
     */
    void deleteCache(String requestId, String cacheId, Consumer<DeleteCacheResult<I>> consumer);

    /**
     * Send a message to remove a cache entry.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're removing an entry from.
     * @param key       The key of the entry we're removing.
     * @param c         The consumer that will handle the result.
     */
    void removeCacheEntry(String requestId, String cacheId, String key, Consumer<RemoveCacheEntryResult<I, K>> c);

    /**
     * Send a message to clear a cache.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're clearing.
     * @param c         The consumer that will handle the result.
     */
    void clearCache(String requestId, String cacheId, Consumer<ClearCacheResult<I>> c);

    /**
     * Send a message to get all cache items.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we're getting an entry from.
     * @param c         The consumer that will handle the result.
     */
    void getCacheEntries(String requestId, String cacheId, Consumer<GetAllCacheEntriesResult<I, K, V>> c);

    /**
     * Send a message to get all cache stats.
     *
     * @param requestId The request ID.
     * @param c         The consumer that will handle the result.
     */
    void getAllCacheStats(String requestId, Consumer<CacheStatsResult<I>> c);

    /**
     * Send a message to subscribe to cache updates.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache to subscribe too.
     * @param c         The consumer that will handle the result.
     */
    void sendCacheSubscribe(String requestId, String cacheId, Consumer<CacheSubscriptionResult<I>> c);

    /**
     * Send a message to unsubscribe to cache updates.
     *
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache to unsubscribe from.
     * @param c         The consumer that will handle the result.
     */
    void sendCacheUnsubscribe(String requestId, String cacheId, Consumer<CacheUnsubscribeResult<I>> c);
}
