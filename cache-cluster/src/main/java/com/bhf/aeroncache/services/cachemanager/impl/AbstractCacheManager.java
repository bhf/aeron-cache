package com.bhf.aeroncache.services.cachemanager.impl;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheFactory;
import com.bhf.aeroncache.services.cachemanager.CacheManager;

import java.util.function.Supplier;

/**
 * Encapsulates flyweights for results of cache manager operations.
 *
 * @param <I> The type on which the individual caches are indexed.
 * @param <K> The type of the key of cache entries.
 * @param <V> The type of the value of cache entries.
 */
public abstract class AbstractCacheManager<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheManager<I, K, V> {

    final CreateCacheResult<I> cacheCreationResult;
    final ClearCacheResult<I> clearCacheResult;
    final DeleteCacheResult<I> deleteCacheResult;
    final RemoveCacheEntryResult<I, K> removeCacheEntryResult;
    final GetCacheEntryResult<I, K, V> getCacheEntryResult;
    final GetAllCacheEntriesResult<I, K, V> getAllCacheEntriesResult;
    final CacheFactory<I, K, V> cacheFactory = new CacheFactory<>();
    final Supplier<I> indexSupplier;
    final Supplier<K> keySupplier;
    final Supplier<V> valueSupplier;

    public AbstractCacheManager(Supplier<I> cacheIndexSupplier, Supplier<K> cacheKeySupplier, Supplier<V> cacheValueSupplier) {
        this.cacheCreationResult = new CreateCacheResult<>(cacheIndexSupplier.get());
        this.clearCacheResult = new ClearCacheResult<>(cacheIndexSupplier.get());
        this.deleteCacheResult = new DeleteCacheResult<>(cacheIndexSupplier.get());
        this.removeCacheEntryResult = new RemoveCacheEntryResult<>(cacheIndexSupplier.get(), cacheKeySupplier.get());
        this.getCacheEntryResult = new GetCacheEntryResult<>(cacheIndexSupplier.get(), cacheKeySupplier.get(), cacheValueSupplier.get());
        this.getAllCacheEntriesResult = new GetAllCacheEntriesResult<>(cacheIndexSupplier.get());
        this.indexSupplier = cacheIndexSupplier;
        this.keySupplier = cacheKeySupplier;
        this.valueSupplier = cacheValueSupplier;
    }

}
