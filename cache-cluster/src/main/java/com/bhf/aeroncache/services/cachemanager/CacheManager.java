package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.Cache;
import io.aeron.ExclusivePublication;
import io.aeron.Image;

/**
 * Top level cache manager interface.
 *
 * @param <I> The type the individual caches are indexed on.
 * @param <K> The type of the key of each individual cache.
 * @param <V> The type of the value of each individual cache.
 */
public interface CacheManager<I extends Reusable, K extends Reusable, V extends Reusable> {

    /**
     * Create a cache.
     *
     * @param cacheId The id of the cache to create.
     * @return The result of creating the cache.
     */
    CreateCacheResult<I> createCache(I cacheId);

    /**
     * Get an existing cache.
     *
     * @param cacheId The id of the cache to get.
     * @return The cache requested.
     */
    Cache<I, K, V> getCache(I cacheId);

    /**
     * Take a snapshot of all caches being managed by this cache manager and
     * serialize them to an {@link ExclusivePublication}.
     *
     * @param snapshotPublication The ExclusivePublication to serialize the caches too.
     */
    void takeSnapshot(ExclusivePublication snapshotPublication);

    /**
     * Load all caches from a snapshot image.
     *
     * @param snapshotImage The snapshot image to load from.
     */
    void loadSnapshot(Image snapshotImage);

    /**
     * Clear the requested cache of all entries.
     *
     * @param cacheId The id of the cache to clear.
     * @return The result of clearing the cache.
     */
    ClearCacheResult<I> clearCache(I cacheId);

    /**
     * Delete the requested cache.
     *
     * @param cacheId The id of the cache to delete.
     * @return The result of deleting the cache.
     */
    DeleteCacheResult<I> deleteCache(I cacheId);

    /**
     * Remove a specific cache entry.
     *
     * @param cacheId The id of the cache we're removing from.
     * @param key     The key for the entry we want to remove.
     * @return The result of removing the entry.
     */
    RemoveCacheEntryResult<I, K> removeCacheEntry(I cacheId, K key);

    /**
     * Get a specific cache entry.
     *
     * @param cacheId The id of the cache we want to get the value from.
     * @param key The key for the entry we want.
     * @return The result of getting the entry from the cache.
     */
    GetCacheEntryResult<I, K, V> getCacheEntry(I cacheId, K key);

    /**
     * Get all cache entries.
     *
     * @param cacheId The id of the cache we want to get the values from.
     * @return The result of getting the entries from the cache.
     */
    GetAllCacheEntriesResult<I, K, V> getAllCacheEntries(I cacheId);

    /**
     * Get stats from all caches.
     * @return The cache stats.
     */
    CacheStatsResult<I> getCacheStatsResult();
}
