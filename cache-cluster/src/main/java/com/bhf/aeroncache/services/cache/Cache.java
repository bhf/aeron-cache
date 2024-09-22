package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;

public interface Cache<K,V> {

    AddCacheEntryResult add(K key, V value);
    RemoveCacheEntryResult remove(K key);
    ClearCacheResult clearEntries();
}
