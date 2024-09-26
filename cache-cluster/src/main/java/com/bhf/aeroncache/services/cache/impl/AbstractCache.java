package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.services.cache.Cache;

/**
 * Holds flyweights for returning results of actions.
 *
 * @param <K> The type of the key of entries in the cache.
 * @param <V> The type of the value of entries in the cache.
 */
public abstract class AbstractCache<I, K, V> implements Cache<I, K, V> {
    final AddCacheEntryResult<I, K> addCacheEntryResult = new AddCacheEntryResult<>();
    final RemoveCacheEntryResult<I, K> removeCacheEntryResult = new RemoveCacheEntryResult<>();
    final ClearCacheResult<I> clearCacheResult = new ClearCacheResult<>();
}
