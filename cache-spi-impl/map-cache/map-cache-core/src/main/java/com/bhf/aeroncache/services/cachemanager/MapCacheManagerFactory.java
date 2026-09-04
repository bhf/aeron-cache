package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.codecs.CacheTimersCodec;
import com.bhf.aeroncache.codecs.request.CacheRequestDecoder;
import com.bhf.aeroncache.codecs.request.CountersCacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.CacheResponseEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseEncoder;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.utils.SupplierUtils;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.HashMap;
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
    private final CacheEntrySnapshotCodec<K, ReusableLong> cacheCountersEntrySnapshotCodec;
    private final CacheResponseEncoder<I,K,V> cacheResponseEncoder;
    private final CacheRequestDecoder<I,K,V> cacheRequestDecoder;
    private final CacheTimersCodec<I, K> timersCodec;
    private final CountersCacheResponseEncoder<I,K,ReusableLong> countersCacheResponseEncoder;
    private final CountersCacheRequestDecoder<I,K,ReusableLong> countersCacheRequestDecoder;


    @Override
    public CacheManager<I, K, V> getCacheManager() {
        return new MapCacheManager<>(cacheIndexSupplier, cacheKeySupplier,
                cacheValueSupplier, mapSupplier, cacheIdSnapshotCodec, cacheEntrySnapshotCodec);
    }

    @Override
    public CountersCacheManager<I, K, ReusableLong> getCountersCacheManager() {
        return new MapCountersCacheManager<>(cacheIndexSupplier, cacheKeySupplier,
                SupplierUtils.longSupplier, HashMap::new, cacheIdSnapshotCodec, cacheCountersEntrySnapshotCodec);
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
        return cacheResponseEncoder;
    }

    @Override
    public CacheRequestDecoder<I, K, V> getCacheRequestDecoder() {
        return cacheRequestDecoder;
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

    @Override
    public CountersCacheRequestDecoder<I, K, ReusableLong> getCountersRequestDecoder() {
        return this.countersCacheRequestDecoder;
    }

    @Override
    public CountersCacheResponseEncoder<I, K, ReusableLong> getCountersResponseEncoder() {
        return this.countersCacheResponseEncoder;
    }
}
