package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.types.ReusableString;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.List;
import java.util.UUID;

public class CountersClearCacheTest extends AbstractClearCacheTest<ReusableString, ReusableString, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    private static final Header header = new Header(0, 0);
    public static final CountersCacheResponseDecoder<ReusableString, ReusableString, ReusableLong> cacheResponseDecoder = new ReusableStringCountersCacheResponseDecoder();
    public static final CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> cacheRequestEncoder = new ReusableStringCountersCacheRequestEncoder();

    public CountersClearCacheTest() {
        super(cacheManagerFactory_);
    }

    @Override
    protected void createCache(ReusableString cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut) {
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeCreateCacheRequest(requestId, cacheId, requestBuffer);
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
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

    @Override
    protected CacheSubscriptionService getSubscriptionServiceToVerify() {
        return sut.countersSubscriptionService;
    }
}
