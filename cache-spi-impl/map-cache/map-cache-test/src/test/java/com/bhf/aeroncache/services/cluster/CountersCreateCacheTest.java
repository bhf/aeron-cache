package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.List;

public class CountersCreateCacheTest extends AbstractCreateCacheTest<ReusableString, ReusableString, ReusableString> {


    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    public static final CountersCacheResponseDecoder<ReusableString, ReusableString, ReusableLong> cacheResponseDecoder = new ReusableStringCountersCacheResponseDecoder();
    public static final CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> cacheRequestEncoder = new ReusableStringCountersCacheRequestEncoder();

    public CountersCreateCacheTest() {
        super(cacheManagerFactory_);
    }

    @Override
    public void decodeCreate(DirectBuffer responseBuffer_, CreateCacheResult<ReusableString> result_) {
        cacheResponseDecoder.decodeCacheCreated(responseBuffer_, 0, result_);
    }

    @Override
    public int encodeCreate(ReusableString cacheId, String requestId, MutableDirectBuffer requestBuffer_) {
        return cacheRequestEncoder.encodeCreateCacheRequest(requestId, cacheId, requestBuffer_);
    }

    protected ReusableString getDuplicateCacheId() {
        return ReusableString.build("123");
    }

    protected List<ReusableString> getCacheIdsToCreate() {
        return List.of(ReusableString.build("asd"), ReusableString.build("****"));
    }
}
