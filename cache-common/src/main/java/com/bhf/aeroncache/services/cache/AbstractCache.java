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
    protected final AddCacheEntryResult<I, K> addCacheEntryResult;
    protected final RemoveCacheEntryResult<I, K> removeCacheEntryResult;
    protected final ClearCacheResult<I> clearCacheResult;
    protected final GetCacheEntryResult<I, K, V> getCacheEntryResult;
    protected final PatchValueResult<I, K, V> patchValueResult;
    protected final Supplier<I> indexSupplier;
    protected final Supplier<K> keySupplier;
    protected final Supplier<V> valueSupplier;
    protected final CacheStats<I> stats;

    public AbstractCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier) {
        this.addCacheEntryResult = new AddCacheEntryResult<>(indexSupplier.get(), keySupplier.get());
        this.removeCacheEntryResult = new RemoveCacheEntryResult<>(indexSupplier.get(), keySupplier.get());
        this.clearCacheResult = new ClearCacheResult<>(indexSupplier.get());
        this.getCacheEntryResult = new GetCacheEntryResult<>(indexSupplier.get(), keySupplier.get(), valueSupplier.get());
        this.patchValueResult = new PatchValueResult<>(indexSupplier.get(), keySupplier.get(), valueSupplier.get());
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
