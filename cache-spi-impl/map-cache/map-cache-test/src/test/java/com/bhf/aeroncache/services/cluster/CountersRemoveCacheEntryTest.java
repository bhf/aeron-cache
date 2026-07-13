package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.UUID;

public class CountersRemoveCacheEntryTest extends AbstractRemoveCacheEntryTest<ReusableString, ReusableString, ReusableLong, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    private static final Header header = new Header(0, 0);
    private static final CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> cacheRequestEncoder = new ReusableStringCountersCacheRequestEncoder();
    private static final CountersCacheResponseDecoder<ReusableString, ReusableString, ReusableLong> cacheResponseDecoder = new ReusableStringCountersCacheResponseDecoder();

    public CountersRemoveCacheEntryTest() {
        super(cacheManagerFactory_);
    }

    @Override
    protected RemoveCacheEntryResult<ReusableString, ReusableString> createResult() {
        return new RemoveCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
    }

    @Override
    protected void createCache(ReusableString cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut) {
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeCreateCacheRequest(requestId, cacheId, requestBuffer);
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
    }

    @Override
    public int encodeAddCacheEntry(String requestId, ReusableString cacheId, ReusableString key, ReusableLong value, long ttl, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, buffer);
    }

    @Override
    public int encodeRemoveCacheEntry(String requestId, ReusableString cacheId, ReusableString key, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeRemoveCacheEntry(requestId, cacheId, key, buffer);
    }

    @Override
    public void decodeCacheEntryRemoved(DirectBuffer buffer, int offset, RemoveCacheEntryResult<ReusableString, ReusableString> result) {
        cacheResponseDecoder.decodeCacheEntryRemoved(buffer, offset, result);
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
    protected ReusableLong getValue() {
        var v = new ReusableLong();
        v.copyFrom(42L);
        return v;
    }

    @Override
    protected CacheSubscriptionService getSubscriptionServiceToVerify() {
        return sut.countersSubscriptionService;
    }
}
