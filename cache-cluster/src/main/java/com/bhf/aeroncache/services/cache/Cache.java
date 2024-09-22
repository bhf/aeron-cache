package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.AddCacheEntryResult;
import com.bhf.aeroncache.models.CacheClearResult;
import com.bhf.aeroncache.models.RemoveCacheEntryResult;

public interface Cache<K,V> {

    AddCacheEntryResult add(K key, V value);
    RemoveCacheEntryResult remove(K key);
    CacheClearResult clearEntries();
}
