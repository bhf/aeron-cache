package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.List;

public class RegularCreateCacheTest extends AbstractCreateCacheTest<ReusableString, ReusableString, ReusableString> {


    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    public static final CacheResponseDecoder<ReusableString, ReusableString, ReusableString> cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    public static final CacheRequestEncoder<ReusableString, ReusableString, ReusableString> cacheRequestEncoder = new ReusableStringCacheRequestEncoder();

    public RegularCreateCacheTest() {
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

    @Override
    protected ReusableString getDuplicateCacheId() {
        return ReusableString.build("123");
    }

    @Override
    protected List<ReusableString> getCacheIdsToCreate() {
        return List.of(ReusableString.build("asd"), ReusableString.build("****"));
    }
}
