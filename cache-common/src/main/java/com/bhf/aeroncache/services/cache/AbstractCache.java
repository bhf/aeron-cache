package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;

import java.util.function.Supplier;

/**
 * Holds flyweights for returning results of actions.
 *
 * @param <I> The type on which the cache is indexed/keyed.
 * @param <K> The type of the key of entries in the cache.
 * @param <V> The type of the value of entries in the cache.
 */
public abstract class AbstractCache<I extends Reusable, K extends Reusable, V extends Reusable> implements Cache<I, K, V> {
    final AddCacheEntryResult<I, K> addCacheEntryResult;
    final RemoveCacheEntryResult<I, K> removeCacheEntryResult;
    final ClearCacheResult<I> clearCacheResult;
    final GetCacheEntryResult<I, K, V> getCacheEntryResult;
    final Supplier<I> indexSupplier;
    final Supplier<K> keySupplier;
    final Supplier<V> valueSupplier;
    final CacheStats<I> stats;

    public AbstractCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier) {
        this.addCacheEntryResult = new AddCacheEntryResult<>(indexSupplier.get(), keySupplier.get());
        this.removeCacheEntryResult = new RemoveCacheEntryResult<>(indexSupplier.get(), keySupplier.get());
        this.clearCacheResult = new ClearCacheResult<>(indexSupplier.get());
        this.getCacheEntryResult = new GetCacheEntryResult<>(indexSupplier.get(), keySupplier.get(), valueSupplier.get());
        this.stats = new CacheStats<>(indexSupplier.get());
        this.indexSupplier = indexSupplier;
        this.keySupplier = keySupplier;
        this.valueSupplier = valueSupplier;
    }

    @Override
    public CacheStats getCacheStats() {
        return stats;
    }
}
