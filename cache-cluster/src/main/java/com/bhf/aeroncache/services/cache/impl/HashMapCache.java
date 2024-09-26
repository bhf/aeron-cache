package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;

import java.util.HashMap;
import java.util.Map;

public class HashMapCache<I,K,V> extends AbstractCache<I,K,V> {

    Map<K,V> cache=new HashMap<>();

    @Override
    public AddCacheEntryResult<I,K> add(K key, V value) {
        addCacheEntryResult.clear();
        addCacheEntryResult.setEntryAdded(true);
        cache.put(key, value);
        return addCacheEntryResult;
    }

    @Override
    public RemoveCacheEntryResult<I,K> remove(K key) {
        removeCacheEntryResult.clear();
        cache.remove(key);
        return removeCacheEntryResult;
    }

    @Override
    public ClearCacheResult<I> clearEntries() {
        clearCacheResult.clear();
        cache.clear();
        return clearCacheResult;
    }
}
