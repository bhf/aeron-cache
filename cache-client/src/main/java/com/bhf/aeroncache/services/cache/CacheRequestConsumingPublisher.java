package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;

import java.util.function.Consumer;

/**
 * An interface for sending and consuming the results of an AeronCache cluster request via Consumers.
 */
public interface CacheRequestConsumingPublisher {
    /**
     * Send a message to create a cache instance.
     * Passes the result to the Consumer.
     *
     * @param cacheId   The ID of the cache to create.
     * @param consumer  The consumer of the result.
     * @param requestId The request ID.
     */
    void sendCreateCache(long cacheId, Consumer<CreateCacheResult<ReusableLong>> consumer, String requestId);

    /**
     * Send a message to add a cache entry.
     *
     * @param cacheId   The ID of the cache we're adding too.
     * @param key       The key to use.
     * @param value     The value to use.
     * @param c         The consumer that will handle the result.
     * @param requestId The request ID.
     */
    void addCacheEntry(long cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c, String requestId);

    /**
     * Send a message to get a cache entry.
     *
     * @param cacheId   The ID of the cache we're getting from.
     * @param key       The key to use.
     * @param c         The consumer to handle the result.
     * @param requestId The request ID.
     */
    void getCacheEntry(long cacheId, String key, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c, String requestId);

    /**
     * Send a message to delete a cache.
     *
     * @param cacheId   The ID of the cache we're deleting.
     * @param requestId The request ID.
     */
    void deleteCache(long cacheId, Consumer<DeleteCacheResult<ReusableLong>> consumer, String requestId);

    /**
     * Send a message to remove a cache entry.
     *
     * @param cacheId   The ID of the cache we're removing an entry from.
     * @param key       The key of the entry we're removing.
     * @param c         The consumer that will handle the result.
     * @param requestId The request ID.
     */
    void removeCacheEntry(long cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c, String requestId);

    /**
     * Send a message to clear a cache.
     *
     * @param cacheId   The ID of the cache we're clearing.
     * @param c         The consumer that will handle the result.
     * @param requestId The request ID.
     */
    void clearCache(long cacheId, Consumer<ClearCacheResult<ReusableLong>> c, String requestId);

    /**
     * Send a message to get all cache items.
     *
     * @param cacheId   The ID of the cache we're getting an entry from.
     * @param c         The consumer that will handle the result.
     * @param requestId The request ID.
     */
    void getCacheEntries(long cacheId, Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> c, String requestId);

    /**
     * Send a message to get all cache stats.
     *
     * @param c         The consumer that will handle the result.
     * @param requestId The request ID.
     */
    void getAllCacheStats(Consumer<CacheStatsResult<ReusableLong>> c, String requestId);

    /**
     * Send a message to subscribe to cache updates.
     *
     * @param cacheId   The ID of the cache to subscribe too.
     * @param c         The consumer that will handle the result.
     * @param requestId The request ID.
     */
    void sendCacheSubscribe(long cacheId, Consumer<CacheSubscriptionResult<ReusableLong>> c, String requestId);

    /**
     * Send a message to unsubscribe to cache updates.
     *
     * @param cacheId   The ID of the cache to unsubscribe from.
     * @param c         The consumer that will handle the result.
     * @param requestId The request ID.
     */
    void sendCacheUnsubscribe(long cacheId, Consumer<CacheUnsubscribeResult<ReusableLong>> c, String requestId);
}
