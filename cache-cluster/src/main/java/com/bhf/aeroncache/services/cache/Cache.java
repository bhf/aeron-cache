package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.AddCacheEntryResult;

public interface Cache<K,V> {

    AddCacheEntryResult add(K key, V value);
}
