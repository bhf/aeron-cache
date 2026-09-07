package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;

/**
 * Handle responses from an Aeron Cache.
 */
public interface CacheResponseHandler<I extends Reusable, K extends Reusable, V extends Reusable> {
    /**
     * Handle a message indicating the value of a get operation on a particular key
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntryResult The result of getting something from the cache.
     */
    void handleCacheEntryResult(GetCacheEntryResult<I, K, V> getCacheEntryResult);

    /**
     * Handle a message indicating the values of an entire cache
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntriesResult The result of getting all items from the cache.
     */
    void handleAllCacheEntries(GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult);

    /**
     * Handle a message indicating a cache has been created and delegate it
     * to any relevant consumer.
     *
     * @param createCacheResult The result of creating a cache.
     */
    void handleCacheCreated(CreateCacheResult<I> createCacheResult);

    /**
     * Handle a message indicating a cache entry has been created and delegate it
     * to any relevant consumer.
     *
     * @param addCacheEntryResult The result of adding an entry to the cache.
     */
    void handleCacheEntryCreated(AddCacheEntryResult<I, K> addCacheEntryResult);

    /**
     * Handle a message indicating a cache entry has been removed and delegate it
     * to any relevant consumer.
     *
     * @param removeCacheEntryResult The result of a cache entry removal.
     */
    void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult);

    /**
     * Handle a message indicating a cache entry has been patched and delegate it
     * to any relevant consumer.
     *
     * @param patchValueResult The result of patching a cache entry.
     */
    default void handleCacheEntryPatched(PatchValueResult<I, K, V> patchValueResult) {}

    /**
     * Handle a message indicating a cache has been cleared and delegate it
     * to any relevant consumer.
     *
     * @param clearCacheResult The result of clearing a cache.
     */
    void handleCacheCleared(ClearCacheResult<I> clearCacheResult);

    /**
     * Handle a message indicating a cache has been deleted and delegate it
     * to any relevant consumer.
     *
     * @param deleteCacheResult The result of deleting a cache.
     */
    void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult);

    /**
     * Handle a message with all cache stats, delegating it
     * to any relevant consumer.
     *
     * @param statsResult The result of getting all cache stats.
     */
    void handleAllCacheStats(CacheStatsResult<I> statsResult);

    /**
     * Handle a message about a subscription request to a cache.
     *
     * @param cacheSubscriptionResult The result of subscribing to a cache.
     */
    void handleCacheSubscribeResponse(CacheSubscriptionResult<I,K,V> cacheSubscriptionResult);

    /**
     * Handle a message about an unsubscribe request to a cache.
     *
     * @param cacheUnsubscribeResult The result of unsubscribing to a cache.
     */
    void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<I> cacheUnsubscribeResult);

    /**
     * Handle a message about a cache entry being updated.
     *
     * @param cacheEntryUpdateResult The entry update details.
     */
    void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult);

    /**
     * Handle a response on a bulk operation request.
     *
     * @param bulkCacheOpsResult The bulk operation result details.
     */
    void handleBulkOperationsResult(BulkCacheOpsResult<I,K,V> bulkCacheOpsResult);

    /**
     * Handle a counter increment result.
     *
     * @param result The counter increment result.
     */
    default void handleCounterIncremented(IncrementCounterResult<I, K> result) {}

    /**
     * Handle a counter decrement result.
     *
     * @param result The counter decrement result.
     */
    default void handleCounterDecremented(DecrementCounterResult<I, K> result) {}

    /**
     * Handle a set counter result.
     *
     * @param result The set counter result.
     */
    default void handleCounterSet(SetCounterResult<I, K> result) {}

    /**
     * Handle a counter cache entry result.
     *
     * @param result The counter cache entry result.
     */
    default void handleCounterCacheEntryResult(GetCacheEntryResult<I, K, ReusableLong> result) {}

    /**
     * Handle all counter cache entries result.
     *
     * @param result The counter cache entries result.
     */
    default void handleAllCounterCacheEntries(GetAllCacheEntriesResult<I, K, ReusableLong> result) {}

    /**
     * Handle a counter cache subscription response.
     *
     * @param result The counter subscription result.
     */
    default void handleCounterCacheSubscribeResponse(CacheSubscriptionResult<I, K, ReusableLong> result) {}
}
