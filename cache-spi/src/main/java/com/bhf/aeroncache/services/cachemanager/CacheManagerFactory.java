package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.codecs.CacheTimersCodec;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.codecs.request.CacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.CacheResponseEncoder;
import com.bhf.aeroncache.models.ReusableLong;

import java.util.Comparator;
import java.util.function.Supplier;

public interface CacheManagerFactory<I extends Reusable, K extends Reusable, V extends Reusable> {

    CacheManager<I, K, V> getCacheManager();

    CountersCacheManager<I, K, ReusableLong> getCountersCacheManager();

    Supplier<I> getIndexSupplier();

    Supplier<K> getKeySupplier();

    Supplier<V> getValueSupplier();

    CacheResponseEncoder<I, K, V> getCacheResponseEncoder();

    CacheRequestDecoder<I, K, V> getCacheRequestDecoder();

    CacheTimersCodec<I, K> getCacheTimersCodec();

    CacheSchemaDetailsProvider getSchemaDetailsProvider();

    Comparator<K> getKeyComparator();
}
