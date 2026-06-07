package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.codecs.CacheTimersCodec;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.bhf.aeroncache.codecs.request.CacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.CacheResponseEncoder;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A factory to create cache manager instances.
 *
 * @param <I> The type on which caches are indexed.
 * @param <K> The type of the key for cache entries which this factory will create.
 * @param <V> The type of the value for cache entries which this factory will create.
 */
@RequiredArgsConstructor
public class MapCacheManagerFactory<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheManagerFactory<I, K, V> {

    private final Supplier<I> cacheIndexSupplier;
    private final Supplier<K> cacheKeySupplier;
    private final Supplier<V> cacheValueSupplier;
    private final Supplier<Map<K, V>> mapSupplier;
    private final CacheIdSnapshotCodec<I> cacheIdSnapshotCodec;
    private final CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec;
    private final CacheResponseEncoder<I,K,V> encoder;
    private final CacheRequestDecoder<I,K,V> decoder;
    private final CacheTimersCodec<I, K> timersCodec;

    @Override
    public CacheManager<I, K, V> getCacheManager() {
        return new MapCacheManager<I, K, V>(cacheIndexSupplier, cacheKeySupplier,
                cacheValueSupplier, mapSupplier, cacheIdSnapshotCodec, cacheEntrySnapshotCodec);
    }

    @Override
    public Supplier<I> getIndexSupplier() {
        return cacheIndexSupplier;
    }

    @Override
    public Supplier<K> getKeySupplier() {
        return cacheKeySupplier;
    }

    @Override
    public Supplier<V> getValueSupplier() {
        return cacheValueSupplier;
    }

    @Override
    public CacheResponseEncoder<I, K, V> getCacheResponseEncoder() {
        return encoder;
    }

    @Override
    public CacheRequestDecoder<I, K, V> getCacheRequestDecoder() {
        return decoder;
    }

    @Override
    public CacheTimersCodec<I, K> getCacheTimersCodec() {
        return timersCodec;
    }

    @Override
    public CacheSchemaDetailsProvider getSchemaDetailsProvider() {
        return new MapCacheSchemaDetailsProvider();
    }

    @Override
    public Comparator<K> getKeyComparator() {
        return cacheEntrySnapshotCodec.getKeyComparator();
    }
}
