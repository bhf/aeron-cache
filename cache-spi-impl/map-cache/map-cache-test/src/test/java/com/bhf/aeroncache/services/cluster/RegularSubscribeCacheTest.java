package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.results.CacheSubscriptionResult;
import com.bhf.aeroncache.models.results.CacheUnsubscribeResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionServiceImpl;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.List;
import java.util.function.Supplier;

public class RegularSubscribeCacheTest extends AbstractSubscribeCacheTest<ReusableString, ReusableString, ReusableString, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    private static final CacheRequestEncoder<ReusableString, ReusableString, ReusableString> cacheRequestEncoder = new ReusableStringCacheRequestEncoder();
    private static final CacheResponseDecoder<ReusableString, ReusableString, ReusableString> cacheResponseDecoder = new ReusableStringCacheResponseDecoder();

    public RegularSubscribeCacheTest() {
        super(cacheManagerFactory_);
    }

    @Override
    protected CacheSubscriptionResult<ReusableString, ReusableString, ReusableString> createResult() {
        return new CacheSubscriptionResult<>(SupplierUtils.stringSupplier.get());
    }

    @Override
    protected void setupSubscriptionService(SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut, IdleStrategy idleStrategy) {
        CacheSubscriptionResult<ReusableString, ReusableString, ReusableString> subscriptionResult = new CacheSubscriptionResult<>(new ReusableString());
        CacheUnsubscribeResult<ReusableString> unsubscribeResult = new CacheUnsubscribeResult<>(new ReusableString());
        Supplier<ReusableString> indexSupplier = ReusableString::new;
        sut.subscriptionService = new CacheSubscriptionServiceImpl<>(idleStrategy, subscriptionResult, unsubscribeResult, indexSupplier, ReusableString::new);
    }

    @Override
    protected void createCache(ReusableString cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut) {
        TestUtils.createCache(cacheId.value(), session, requestBuffer, sut);
    }

    @Override
    public int encodeCacheSubscribe(String requestId, List<ReusableString> cacheIds, boolean sendSnapshot, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeCacheSubscribe(requestId, cacheIds, sendSnapshot, buffer);
    }

    @Override
    public int encodeAddCacheEntry(String requestId, ReusableString cacheId, ReusableString key, ReusableString value, long ttl, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, buffer);
    }

    @Override
    public void decodeCacheSubscribeResult(DirectBuffer buffer, int offset, CacheSubscriptionResult<ReusableString, ReusableString, ReusableString> result) {
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
    protected ReusableString getHydrationValue() {
        return ReusableString.build("value");
    }
}
