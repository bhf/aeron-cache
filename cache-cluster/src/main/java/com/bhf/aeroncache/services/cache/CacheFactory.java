package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.services.cache.impl.HashMapCache;

public class CacheFactory<K,V> {
    public Cache<K,V> getNewCache() {
        return new HashMapCache<K,V>();
    }
}
