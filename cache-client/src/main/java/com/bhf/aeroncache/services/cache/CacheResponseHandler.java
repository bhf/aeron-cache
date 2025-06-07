package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;

/**
 * Handle responses from an Aeron Cache.
 */
public interface CacheResponseHandler {
    /**
     * Handle a message indicating the value of a get operation on a particular key
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntryResult The result of getting something from the cache.
     */
    void handleCacheEntryResult(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult);

    /**
     * Handle a message indicating the values of an entire cache
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntriesResult The result of getting all items from the cache.
     */
    void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult);

    /**
     * Handle a message indicating a cache has been created and delegate it
     * to any relevant consumer.
     *
     * @param createCacheResult The result of creating a cache.
     */
    void handleCacheCreated(CreateCacheResult<ReusableLong> createCacheResult);

    /**
     * Handle a message indicating a cache entry has been created and delegate it
     * to any relevant consumer.
     *
     * @param addCacheEntryResult The result of adding an entry to the cache.
     */
    void handleCacheEntryCreated(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult);

    /**
     * Handle a message indicating a cache entry has been removed and delegate it
     * to any relevant consumer.
     *
     * @param removeCacheEntryResult The result of a cache entry removal.
     */
    void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult);

    /**
     * Handle a message indicating a cache has been cleared and delegate it
     * to any relevant consumer.
     *
     * @param clearCacheResult The result of clearing a cache.
     */
    void handleCacheCleared(ClearCacheResult<ReusableLong> clearCacheResult);

    /**
     * Handle a message indicating a cache has been deleted and delegate it
     * to any relevant consumer.
     *
     * @param deleteCacheResult The result of deleting a cache.
     */
    void handleCacheDeleted(DeleteCacheResult<ReusableLong> deleteCacheResult);

    /**
     * Handle a message with all cache stats, delegating it
     * to any relevant consumer.
     *
     * @param statsResult The result of getting all cache stats.
     */
    void handleAllCacheStats(CacheStatsResult<ReusableLong> statsResult);

    /**
     * Handle a message about a subscription request to a cache.
     *
     * @param cacheSubscriptionResult The result of subscribing to a cache.
     */
    void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult);

    /**
     * Handle a message about an unsubscribe request to a cache.
     *
     * @param cacheUnsubscribeResult The result of unsubscribing to a cache.
     */
    void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult);

    void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult);
}
