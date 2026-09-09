package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.SetCounterResult;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SetCounterTest {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory = com.bhf.aeroncache.services.TestUtils.getCacheManagerFactory();
    private static final Header header = new Header(0, 0);
    private static final CountersCacheResponseDecoder<ReusableString, ReusableString, ReusableLong> cacheResponseDecoder = new ReusableStringCountersCacheResponseDecoder();
    private static final CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> cacheRequestEncoder = new ReusableStringCountersCacheRequestEncoder();

    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private SetCounterResult<ReusableString, ReusableString> result;
    private SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService<>("node0", tracingService, cacheManagerFactory);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new SetCounterResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
    }

    @Test
    @DisplayName("Should set counter on a known cache")
    void shouldSetCounter() {
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        ReusableString cacheId = ReusableString.build("testCacheId");
        ReusableString counterId = ReusableString.build("counter1");
        long initialValue = 42L;
        long newValue = 100L;

        createCounterCache(cacheId, session);
        addCounterEntry(cacheId, counterId, initialValue, session);

        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeSetCounterRequest(requestId, cacheId, counterId, newValue, 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        cacheResponseDecoder.decodeSetCounterResponse(responseBuffer, 0, result);

        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(counterId.value(), result.getKey().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals(newValue, result.getCounterValue());

        verify(sut.countersSubscriptionService, times(1)).handleCounterUpdated(
                any(), any(), any(MutableDirectBuffer.class), anyInt());
    }

    @Test
    @DisplayName("Should return unknown cache when setting counter on non-existent cache")
    void shouldReturnUnknownCacheWhenCacheDoesNotExist() {
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        ReusableString cacheId = ReusableString.build("unknownCache");
        ReusableString counterId = ReusableString.build("counter1");

        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeSetCounterRequest(requestId, cacheId, counterId, 100L, 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        cacheResponseDecoder.decodeSetCounterResponse(responseBuffer, 0, result);

        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

    private void createCounterCache(ReusableString cacheId, ClientSession session) {
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeCreateCacheRequest(requestId, cacheId, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
    }

    private void addCounterEntry(ReusableString cacheId, ReusableString key, long value, ClientSession session) {
        var v = new ReusableLong();
        v.copyFrom(value);
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, v, 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
    }
}
