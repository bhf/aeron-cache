package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.DeleteCacheResult;
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

public class CountersDeleteCacheTest extends AbstractDeleteCacheTest<ReusableString, ReusableString, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    private static final Header header = new Header(0, 0);
    public static final CountersCacheResponseDecoder<ReusableString, ReusableString, ReusableLong> cacheResponseDecoder = new ReusableStringCountersCacheResponseDecoder();
    public static final CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> cacheRequestEncoder = new ReusableStringCountersCacheRequestEncoder();

    public CountersDeleteCacheTest() {
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
    public int encodeDeleteCache(String requestId, ReusableString cacheId, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeDeleteCache(requestId, cacheId, buffer);
    }

    @Override
    public void decodeDeleteCache(DirectBuffer buffer, int offset, DeleteCacheResult<ReusableString> result) {
        cacheResponseDecoder.decodeCacheDeleted(buffer, offset, result);
    }

    @Override
    protected List<ReusableString> getCacheIdsToDelete() {
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
