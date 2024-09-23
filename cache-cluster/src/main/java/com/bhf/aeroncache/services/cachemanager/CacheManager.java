package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.cache.Cache;
import io.aeron.ExclusivePublication;
import io.aeron.Image;

/**
 * Top level cache manager interface.
 * @param <I> The type the individual caches are indexed on.
 * @param <K> The type of the key of each individual cache.
 * @param <V> The type of the value of each individual cache.
 */
public interface CacheManager<I, K,V> {

    /**
     * Create a cache.
     * @param cacheId The id of the cache to create.
     * @return The result of creating the cache.
     */
    CreateCacheResult<I> createCache(I cacheId);

    /**
     * Get an existing cache.
     * @param cacheId The id of the cache to get.
     * @return The cache requested.
     */
    Cache<K,V> getCache(I cacheId);

    /**
     * Take a snapshot of all caches being managed by this cache manager and
     * serialize them to an {@link ExclusivePublication}.
     * @param snapshotPublication The ExclusivePublication to serialize the caches too.
     */
    void takeSnapshot(ExclusivePublication snapshotPublication);

    /**
     * Load all caches from a snapshot image.
     * @param snapshotImage The snapshot image to load from.
     */
    void loadSnapshot(Image snapshotImage);

    /**
     * Clear the requested cache of all entries.
     * @param cacheId The id of the cache to clear.
     * @return The result of clearing the cache.
     */
    ClearCacheResult<I> clearCache(I cacheId);

    /**
     * Delete the requested cache.
     * @param cacheId The id of the cache to delete.
     * @return The deleted cache.
     */
    Cache<K,V> deleteCache(I cacheId);
}
