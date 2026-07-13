package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.CacheSubscriptionResult;
import com.bhf.aeroncache.models.results.CacheUnsubscribeResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionServiceImpl;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class CountersSubscribeCacheTest extends AbstractSubscribeCacheTest<ReusableString, ReusableString, ReusableLong, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    private static final Header header = new Header(0, 0);
    private static final CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> cacheRequestEncoder = new ReusableStringCountersCacheRequestEncoder();
    private static final CountersCacheResponseDecoder<ReusableString, ReusableString, ReusableLong> cacheResponseDecoder = new ReusableStringCountersCacheResponseDecoder();

    public CountersSubscribeCacheTest() {
        super(cacheManagerFactory_);
    }

    @Override
    protected CacheSubscriptionResult<ReusableString, ReusableString, ReusableLong> createResult() {
        return new CacheSubscriptionResult<>(SupplierUtils.stringSupplier.get());
    }

    @Override
    protected void setupSubscriptionService(SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut, IdleStrategy idleStrategy) {
        CacheSubscriptionResult<ReusableString, ReusableString, ReusableLong> subscriptionResult = new CacheSubscriptionResult<>(new ReusableString());
        CacheUnsubscribeResult<ReusableString> unsubscribeResult = new CacheUnsubscribeResult<>(new ReusableString());
        Supplier<ReusableString> indexSupplier = ReusableString::new;
        sut.countersSubscriptionService = new CacheSubscriptionServiceImpl<>(idleStrategy, subscriptionResult, unsubscribeResult, indexSupplier);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
    }

    @Override
    protected void createCache(ReusableString cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut) {
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeCreateCacheRequest(requestId, cacheId, requestBuffer);
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
    }

    @Override
    public int encodeCacheSubscribe(String requestId, List<ReusableString> cacheIds, boolean sendSnapshot, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeCacheSubscribe(requestId, cacheIds, sendSnapshot, buffer);
    }

    @Override
    public int encodeAddCacheEntry(String requestId, ReusableString cacheId, ReusableString key, ReusableLong value, long ttl, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, buffer);
    }

    @Override
    public void decodeCacheSubscribeResult(DirectBuffer buffer, int offset, CacheSubscriptionResult<ReusableString, ReusableString, ReusableLong> result) {
        cacheResponseDecoder.decodeCacheSubscribeResult(buffer, offset, result);
    }

    @Override
    protected ReusableString getCacheId() {
        return ReusableString.build("0");
    }

    @Override
    protected ReusableString getUnknownCacheId() {
        return ReusableString.build("123L");
    }

    @Override
    protected ReusableString getHydrationCacheId() {
        return ReusableString.build("hydration-test-cache");
    }

    @Override
    protected ReusableString getHydrationKey() {
        return ReusableString.build("key");
    }

    @Override
    protected ReusableLong getHydrationValue() {
        var v = new ReusableLong();
        v.copyFrom(42L);
        return v;
    }
}
