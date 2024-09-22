package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.services.cache.Cache;

public abstract class AbstractCache<K, V> implements Cache<K, V> {
    AddCacheEntryResult addCacheEntryResult = new AddCacheEntryResult();
    RemoveCacheEntryResult removeCacheEntryResult = new RemoveCacheEntryResult();
    ClearCacheResult clearCacheResult = new ClearCacheResult();
}
