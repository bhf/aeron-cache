package com.bhf.aeroncache.services.cachemanager.impl;

import com.bhf.aeroncache.models.CacheClearResult;
import com.bhf.aeroncache.models.CacheCreationResult;
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
    public CacheClearResult clearCache(I cacheId) {
        return getCache(cacheId).clearEntries();
    }

    @Override
    public CacheCreationResult createCache(I cacheId) {
        cacheCreationResult.clear();
        var cache = cacheFactory.getNewCache();
        caches.put(cacheId, cache);
        cacheCreationResult.setTimeCreated(System.currentTimeMillis());
        return cacheCreationResult;
    }
}
