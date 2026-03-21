package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.codecs.CacheRequestDecoder;
import com.bhf.aeroncache.codecs.CacheResponseEncoder;

import java.util.function.Supplier;

public interface CacheManagerFactory<I extends Reusable, K extends Reusable, V extends Reusable> {

    CacheManager<I, K, V> getCacheManager();

    Supplier<I> getIndexSupplier();

    Supplier<K> getKeySupplier();

    Supplier<V> getValueSupplier();

    CacheResponseEncoder<I, K, V> getEncoder();

    CacheRequestDecoder<I, K, V> getDecoder();
}
