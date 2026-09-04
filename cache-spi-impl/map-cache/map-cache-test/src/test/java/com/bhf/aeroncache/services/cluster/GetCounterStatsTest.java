package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CountersCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.requests.GetCacheStatsRequestDetails;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CacheStatsResult;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableString;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a request to get all counter cache stats.
 */
class GetCounterStatsTest {

    private static final CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory = com.bhf.aeroncache.services.TestUtils.getCacheManagerFactory();
    private static final Header header = new Header(0, 0);
    private static final CountersCacheResponseDecoder<ReusableString, ReusableString, ReusableLong> cacheResponseDecoder = new ReusableStringCountersCacheResponseDecoder();
    private static final CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> cacheRequestEncoder = new ReusableStringCountersCacheRequestEncoder();

    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private CacheStatsResult<ReusableString> result;
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
        result = new CacheStatsResult<>();
    }

    @Test
    @DisplayName("Should return counter cache stats for known counter caches")
    void shouldGetStatsForKnownCounterCache() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        ReusableString cacheId = ReusableString.build("testCounterCache");
        ReusableString counterId = ReusableString.build("counter1");

        createCounterCache(cacheId, session);
        addCounterEntry(cacheId, counterId, 42L, session);

        var requestId = UUID.randomUUID().toString();
        int length = cacheRequestEncoder.encodeGetAllCacheStats(requestId, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeAllCacheStatsResult(responseBuffer, 0, result);

        // Assert
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getOperationStatus());
        assertEquals(1, result.getStats().size());
        var stats = result.getStats().get(0);
        assertEquals(cacheId.value(), stats.getCacheId().value());
        assertEquals(1L, stats.size);

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startGetAllStatsRequest(any(GetCacheStatsRequestDetails.class));
        verify(tracingService, times(1)).endGetAllStatsRequest(any(GetCacheStatsRequestDetails.class));
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
