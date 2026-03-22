package com.bhf.aeroncache.services.cacheclient;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;

public class MapCacheClientFactory implements CacheClientFactory{
    @Override
    public CacheRequestEncoder getCacheRequestEncoder() {
        return new RegularStringCacheRequestEncoder();
    }

    @Override
    public CacheResponseDecoder getCacheResponseDecoder() {
        return new ReusableStringCacheResponseDecoder();
    }

    @Override
    public CacheClientSchemDetailsProvider getSchemaDetails() {
        return new MapCacheClientSchemDetailsProvider();
    }
}
