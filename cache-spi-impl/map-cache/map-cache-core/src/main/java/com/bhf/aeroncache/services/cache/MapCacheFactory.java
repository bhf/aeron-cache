package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.pooled.PooledMapCache;
import com.bhf.aeroncache.services.cache.pooled.PooledMapEntry;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.bhf.aeroncache.services.patch.JSONPatchProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A factory to return cache instances.
 *
 * <p>The implementation returned is controlled by the {@value #POOLING_ENABLED_ENV_VAR}
 * environment variable. When set to {@code true} (case-insensitive) a {@link PooledMapCache}
 * is returned; otherwise the default {@link MapCache} is returned.</p>
 *
 * @param <I> The type on which this cache is indexed.
 * @param <K> The type of the key for cache entries which this factory will create.
 * @param <V> The type of the value for cache entries which this factory will create.
 */
public class MapCacheFactory<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheFactory<I, K, V> {
    /**
     * Name of the environment variable used to select the pooled cache implementation.
     */
    public static final String POOLING_ENABLED_ENV_VAR = "AERON_CACHE_POOLING_ENABLED";

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Cache<I, K, V> getNewCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier,
                                      Supplier<Map<K, V>> mapSupplier, CacheIdSnapshotCodec<I> cacheIdSnapshotCodec,
                                      CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec) {
        if (isPoolingEnabled()) {
            Supplier<Map<K, PooledMapEntry<K, V>>> pooledMapSupplier = (Supplier) mapSupplier;
            return new PooledMapCache<I, K, V>(indexSupplier, keySupplier, valueSupplier, pooledMapSupplier,
                    cacheIdSnapshotCodec, cacheEntrySnapshotCodec, new JSONPatchProvider<>());
        }
        return new MapCache<I, K, V>(indexSupplier, keySupplier, valueSupplier, mapSupplier,
                cacheIdSnapshotCodec, cacheEntrySnapshotCodec, new JSONPatchProvider<>());
    }

    private static boolean isPoolingEnabled() {
        return Boolean.parseBoolean(System.getenv(POOLING_ENABLED_ENV_VAR));
    }
}
