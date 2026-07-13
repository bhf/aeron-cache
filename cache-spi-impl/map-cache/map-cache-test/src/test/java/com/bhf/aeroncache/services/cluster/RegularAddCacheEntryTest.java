package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.types.ReusableString;
import io.aeron.cluster.service.ClientSession;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class RegularAddCacheEntryTest extends AbstractAddCacheEntryTest<ReusableString, ReusableString, ReusableString, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    private static final CacheResponseDecoder<ReusableString, ReusableString, ReusableString> cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    private static final CacheRequestEncoder<ReusableString, ReusableString, ReusableString> cacheRequestEncoder = new ReusableStringCacheRequestEncoder();

    public RegularAddCacheEntryTest() {
        super(cacheManagerFactory_);
    }

    @Override
    protected void createCache(ReusableString cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut) {
        TestUtils.createCache(cacheId.value(), session, requestBuffer, sut);
    }

    @Override
    public int encodeAddCacheEntry(String requestId, ReusableString cacheId, ReusableString key, ReusableString value, long ttl, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, buffer);
    }

    @Override
    public void decodeAddCacheEntryResult(DirectBuffer buffer, int offset, AddCacheEntryResult<ReusableString, ReusableString> result) {
        cacheResponseDecoder.decodeAddCacheEntryResult(buffer, offset, result);
    }

    @Override
    protected ReusableString getCacheId() {
        return ReusableString.build("testCacheId");
    }

    @Override
    protected ReusableString getRescheduleCacheId() {
        return ReusableString.build("testCacheId-ttl-resched");
    }

    @Override
    protected ReusableString getUnknownCacheId() {
        return ReusableString.build("123L");
    }

    @Override
    protected ReusableString getDynamicCacheId() {
        return ReusableString.build("Some-Uncreated-Cache");
    }

    @Override
    protected ReusableString getKey() {
        return ReusableString.build("someKey★★★");
    }

    @Override
    protected ReusableString getValue() {
        return ReusableString.build("someValue★★★");
    }
}
