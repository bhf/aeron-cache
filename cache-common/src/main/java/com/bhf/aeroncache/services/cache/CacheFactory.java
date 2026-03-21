package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.impl.MapCache;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A factory to return cache instances.
 *
 * @param <I> The type on which this cache is indexed.
 * @param <K> The type of the key for cache entries which this factory will create.
 * @param <V> The type of the value for cache entries which this factory will create.
 */
public class CacheFactory<I extends Reusable, K extends Reusable, V extends Reusable> {
    public Cache<I, K, V> getNewCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier,
                                      Supplier<Map<K, V>> mapSupplier, CacheIdCodec<I> cacheIdSnapshotCodec,
                                      CacheEntryCodec<K, V> cacheEntrySnapshotCodec) {
        return new MapCache<I, K, V>(indexSupplier, keySupplier, valueSupplier, mapSupplier,
                cacheIdSnapshotCodec, cacheEntrySnapshotCodec);
    }
}
