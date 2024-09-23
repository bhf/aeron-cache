package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.services.cache.impl.HashMapCache;

/**
 * A factory to return cache instances.
 * @param <K> The type of the key for cache entries which this factory will create.
 * @param <V> The type of the value for cache entries which this factory will create.
 */
public class CacheFactory<K,V> {
    public Cache<K,V> getNewCache() {
        return new HashMapCache<K,V>();
    }
}
