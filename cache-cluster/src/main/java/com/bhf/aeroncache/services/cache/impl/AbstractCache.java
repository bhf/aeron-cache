package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.services.cache.Cache;

/**
 * Holds flyweights for returning results of actions.
 * @param <K> The type of the key of entries in the cache.
 * @param <V> The type of the value of entries in the cache.
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {
    final AddCacheEntryResult<K> addCacheEntryResult = new AddCacheEntryResult<>();
    final RemoveCacheEntryResult<K> removeCacheEntryResult = new RemoveCacheEntryResult<>();
    final ClearCacheResult<K> clearCacheResult = new ClearCacheResult<>();
}
