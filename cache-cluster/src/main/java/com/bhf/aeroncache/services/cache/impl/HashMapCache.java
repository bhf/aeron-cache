package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.AddCacheEntryResult;

import java.util.HashMap;
import java.util.Map;

public class HashMapCache<K,V> extends AbstractCache<K,V> {

    Map<K,V> cache=new HashMap<>();

    @Override
    public AddCacheEntryResult add(K key, V value) {
        addCacheEntryResult.clear();
        addCacheEntryResult.setEntryAdded(true);
        cache.put(key, value);
        return addCacheEntryResult;
    }
}
