package com.bhf.aeroncache.services.cacheclient;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.models.Reusable;

import java.util.function.Supplier;

/**
 * A factory for creating things which an AeronCache Client needs given a particular
 * implementation of a cache-spi.
 */
public interface CacheClientFactory<I extends Reusable, K extends Reusable, V extends Reusable> {

    CacheRequestEncoder<I,K,V> getCacheRequestEncoder();

    CacheResponseDecoder<I,K,V> getCacheResponseDecoder();

    CacheClientSchemDetailsProvider getSchemaDetails();

    Supplier<I> getIndexSupplier();

    Supplier<K> getKeySupplier();

    Supplier<V> getValueSupplier();
}
