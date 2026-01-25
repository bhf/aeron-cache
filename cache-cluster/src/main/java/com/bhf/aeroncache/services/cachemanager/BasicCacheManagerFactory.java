package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.CacheEntryCodec;
import com.bhf.aeroncache.services.cache.CacheIdCodec;
import com.bhf.aeroncache.services.cachemanager.impl.MapCacheManager;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A factory to create cache manager instances.
 *
 * @param <I> The type on which caches are indexed.
 * @param <K> The type of the key for cache entries which this factory will create.
 * @param <V> The type of the value for cache entries which this factory will create.
 */
public class BasicCacheManagerFactory<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheManagerFactory<I, K, V> {

    private final Supplier<I> cacheIndexSupplier;
    private final Supplier<K> cacheKeySupplier;
    private final Supplier<V> cacheValueSupplier;
    private final Supplier<Map<K, V>> mapSupplier;
    private final CacheIdCodec<I> cacheIdSerializer;
    private final CacheEntryCodec<K, V> cacheEntrySerializer;

    public BasicCacheManagerFactory(Supplier<I> cacheIndexSupplier, Supplier<K> cacheKeySupplier, Supplier<V> cacheValueSupplier, Supplier<Map<K, V>> mapSupplier, CacheIdCodec<I> cacheIdSerializer, CacheEntryCodec<K, V> cacheEntrySerializer) {
        this.cacheIndexSupplier = cacheIndexSupplier;
        this.cacheKeySupplier = cacheKeySupplier;
        this.cacheValueSupplier = cacheValueSupplier;
        this.mapSupplier = mapSupplier;
        this.cacheIdSerializer = cacheIdSerializer;
        this.cacheEntrySerializer = cacheEntrySerializer;
    }

    @Override
    public CacheManager<I, K, V> getCacheManager() {
        return new MapCacheManager<I, K, V>(cacheIndexSupplier, cacheKeySupplier,
                cacheValueSupplier, mapSupplier, cacheIdSerializer, cacheEntrySerializer);
    }
}
