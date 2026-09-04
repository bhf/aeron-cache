package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.types.ReusableString;
import io.aeron.cluster.service.ClientSession;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.List;

public class RegularClearCacheTest extends AbstractClearCacheTest<ReusableString, ReusableString, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    public static final CacheResponseDecoder<ReusableString, ReusableString, ReusableString> cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    public static final CacheRequestEncoder<ReusableString, ReusableString, ReusableString> cacheRequestEncoder = new ReusableStringCacheRequestEncoder();

    public RegularClearCacheTest() {
        super(cacheManagerFactory_);
    }

    @Override
    protected void createCache(ReusableString cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut) {
        TestUtils.createCache(cacheId.value(), session, requestBuffer, sut);
    }

    @Override
    public int encodeClearCache(String requestId, ReusableString cacheId, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeClearCache(requestId, cacheId, buffer);
    }

    @Override
    public void decodeCacheCleared(DirectBuffer buffer, int offset, ClearCacheResult<ReusableString> result) {
        cacheResponseDecoder.decodeCacheCleared(buffer, offset, result);
    }

    @Override
    protected List<ReusableString> getCacheIdsToClear() {
        return List.of(ReusableString.build("testCacheId"), ReusableString.build("★★★★★"));
    }

    @Override
    protected ReusableString getUnknownCacheId() {
        return ReusableString.build("123L");
    }
}
