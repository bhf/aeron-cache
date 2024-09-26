package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.services.cache.impl.HashMapCache;

/**
 * A factory to return cache instances.
 *
 * @param <K> The type of the key for cache entries which this factory will create.
 * @param <V> The type of the value for cache entries which this factory will create.
 */
public class CacheFactory<I, K, V> {
    public Cache<I, K, V> getNewCache() {
        return new HashMapCache<I, K, V>();
    }
}
