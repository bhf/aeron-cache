package com.bhf.aeroncache.services.cachemanager.impl;

import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.cache.Cache;

import java.util.HashMap;

/**
 * A cache manager which indexes cache instances based on a type I.
 * Uses a Java HashMap.
 * @param <I> The type of the id of the cache.
 * @param <K> The key type for the caches.
 * @param <V> The value type for the caches.
 */
public abstract class AbstractHashMapCacheManager<I, K, V> extends AbstractCacheManager<I, K, V>{

    private final HashMap<I, Cache<K,V>> caches=new HashMap<>();

    @Override
    public Cache<K,V> getCache(I cacheId) {
        return caches.get(cacheId);
    }

    @Override
    public ClearCacheResult<I> clearCache(I cacheId) {
        clearCacheResult.clear();
        getCache(cacheId).clearEntries();
        clearCacheResult.setCacheId(cacheId);
        return clearCacheResult;
    }

    @Override
    public CreateCacheResult<I> createCache(I cacheId) {
        cacheCreationResult.clear();
        var cache = cacheFactory.getNewCache();
        caches.put(cacheId, cache);
        cacheCreationResult.setCacheId(cacheId);
        return cacheCreationResult;
    }

    @Override
    public Cache<K, V> deleteCache(I cacheId) {
        deleteCacheResult.clear();
        deleteCacheResult.setCacheId(cacheId);
        return caches.remove(cacheId);
    }
}
