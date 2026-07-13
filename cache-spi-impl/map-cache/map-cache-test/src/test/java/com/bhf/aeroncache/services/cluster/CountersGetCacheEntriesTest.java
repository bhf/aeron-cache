package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.GetAllCacheEntriesResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CountersGetCacheEntriesTest extends AbstractGetCacheEntriesTest<ReusableString, ReusableString, ReusableLong, ReusableString> {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory_ = TestUtils.getCacheManagerFactory();
    private static final Header header = new Header(0, 0);
    private static final CountersCacheResponseDecoder<ReusableString, ReusableString, ReusableLong> cacheResponseDecoder = new ReusableStringCountersCacheResponseDecoder();
    private static final CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> cacheRequestEncoder = new ReusableStringCountersCacheRequestEncoder();

    public CountersGetCacheEntriesTest() {
        super(cacheManagerFactory_);
    }

    @Override
    protected GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableLong> createResult() {
        return new GetAllCacheEntriesResult<>(SupplierUtils.stringSupplier.get());
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
    public int encodeGetCacheEntries(String requestId, ReusableString cacheId, MutableDirectBuffer buffer) {
        return cacheRequestEncoder.encodeGetCacheEntries(requestId, cacheId, buffer);
    }

    @Override
    public void decodeAllCacheEntriesResult(DirectBuffer buffer, int offset, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableLong> result) {
        cacheResponseDecoder.decodeAllCacheEntriesResult(buffer, offset, result);
    }

    @Override
    protected ReusableString getCacheId() {
        return ReusableString.build("testCacheId");
    }

    @Override
    protected ReusableString getUnknownCacheId() {
        return ReusableString.build("123L");
    }

    @Override
    protected List<KeyValue<ReusableString, ReusableLong>> getEntriesToAdd() {
        List<KeyValue<ReusableString, ReusableLong>> entries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            var v = new ReusableLong();
            v.copyFrom((long) i);
            entries.add(new KeyValue<>(ReusableString.build("key-" + i), v));
        }
        return entries;
    }
}
