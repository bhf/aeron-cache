package com.bhf.aeroncache.services.cachemanager.impl;

import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.DeleteCacheResult;
import com.bhf.aeroncache.services.cache.CacheFactory;
import com.bhf.aeroncache.services.cachemanager.CacheManager;

/**
 * Encapsulates flyweights for results of cache manager operations.
 * @param <I> The type on which the individual caches are indexed.
 * @param <K> The type of the key of cache entries.
 * @param <V> The type of the value of cache entries.
 */
public abstract class AbstractCacheManager<I, K, V> implements CacheManager<I, K, V> {

    final CreateCacheResult<I> cacheCreationResult=new CreateCacheResult<>();
    final ClearCacheResult<I> clearCacheResult=new ClearCacheResult<>();
    final DeleteCacheResult<I> deleteCacheResult=new DeleteCacheResult<>();
    final CacheFactory<I,K,V> cacheFactory=new CacheFactory<>();
}
