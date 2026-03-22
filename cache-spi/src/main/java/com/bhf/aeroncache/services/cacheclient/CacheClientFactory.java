package com.bhf.aeroncache.services.cacheclient;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;

/**
 * A factory for creating things which an AeronCache Client needs given a particular
 * implementation of a cache-spi.
 */
public interface CacheClientFactory {
    CacheRequestEncoder getCacheRequestEncoder();
    CacheResponseDecoder getCacheResponseDecoder();
    CacheClientSchemDetailsProvider getSchemaDetails();
}
