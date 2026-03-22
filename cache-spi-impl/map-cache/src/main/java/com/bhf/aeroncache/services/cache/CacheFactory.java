package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;

import java.util.Map;
import java.util.function.Supplier;

/**
 * An optional interface to serve as a creational pattern for your caches.
 * Typically used in a {@link com.bhf.aeroncache.services.cachemanager.CacheManager}
 * when you create a new cache.
 *
 * @param <I> The type of the index for Caches being created.
 * @param <K> The type of the key for Caches being created.
 * @param <V> The type of the value for Caches being created.
 */
public interface CacheFactory<I extends Reusable, K extends Reusable, V extends Reusable> {
    Cache<I, K, V> getNewCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier,
                               Supplier<Map<K, V>> mapSupplier, CacheIdSnapshotCodec<I> cacheIdSnapshotCodec,
                               CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec);
}
