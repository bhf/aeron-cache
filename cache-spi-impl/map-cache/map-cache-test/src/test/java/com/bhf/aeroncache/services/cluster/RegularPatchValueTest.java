package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.models.results.PatchValueResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class RegularPatchValueTest extends AbstractPatchValueTest<ReusableString, ReusableString, ReusableString, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    private static final CacheResponseDecoder<ReusableString, ReusableString, ReusableString> cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    private static final CacheRequestEncoder<ReusableString, ReusableString, ReusableString> cacheRequestEncoder = new ReusableStringCacheRequestEncoder();

    public RegularPatchValueTest() {
        super(cacheManagerFactory_);
    }

    @Override
    protected PatchValueResult<ReusableString, ReusableString, ReusableString> createResult() {
        return new PatchValueResult<>(SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get());
    }

    @Override
    protected GetCacheEntryResult<ReusableString, ReusableString, ReusableString> createGetResult() {
        return new GetCacheEntryResult<>(SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get());
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
    public int encodePatchValue(String requestId, ReusableString cacheId, ReusableString key, ReusableString patch, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodePatchValue(requestId, cacheId, key, patch, buffer);
    }

    @Override
    public int encodeGetCacheEntry(String requestId, ReusableString cacheId, ReusableString key, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeGetCacheEntry(requestId, cacheId, key, buffer);
    }

    @Override
    public void decodePatchValueResult(DirectBuffer buffer, int offset, PatchValueResult<ReusableString, ReusableString, ReusableString> result) {
        cacheResponseDecoder.decodePatchValueResult(buffer, offset, result);
    }

    @Override
    public void decodeGetCacheEntryResult(DirectBuffer buffer, int offset, GetCacheEntryResult<ReusableString, ReusableString, ReusableString> result) {
        cacheResponseDecoder.decodeGetCacheEntryResult(buffer, offset, result);
    }

    @Override
    protected ReusableString getCacheId() {
        return ReusableString.build("testCacheId");
    }

    @Override
    protected ReusableString getUnknownKeyCacheId() {
        return ReusableString.build("123L");
    }

    @Override
    protected ReusableString getKey() {
        return ReusableString.build("someKey★★★");
    }

    @Override
    protected ReusableString getInitialValue() {
        return ReusableString.build("{\"a\":1,\"b\":2}");
    }

    @Override
    protected ReusableString getPatch() {
        return ReusableString.build("{\"b\":3,\"c\":4}");
    }

    @Override
    protected ReusableString getExpectedPatchedValue() {
        return ReusableString.build("{\"a\":1,\"b\":3,\"c\":4}");
    }

    @Override
    protected ReusableString getInvalidPatch() {
        return ReusableString.build("not-json");
    }
}
