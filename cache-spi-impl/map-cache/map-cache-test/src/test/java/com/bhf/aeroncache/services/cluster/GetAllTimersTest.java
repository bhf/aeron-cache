package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.requests.GetAllTimersRequestDetails;
import com.bhf.aeroncache.models.results.AllTimersResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.TimerDetails;
import com.bhf.aeroncache.models.results.TimerType;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableString;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.Header;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test decoding a request to get all pending TTL removal timers. Uses a Detroit style for
 * simplicity in decoding the response buffer.
 */
class GetAllTimersTest {

    private final Header header = new Header(0, 0);
    private final CacheResponseDecoder<ReusableString, ReusableString, ReusableString> cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    private final CacheRequestEncoder<ReusableString, ReusableString, ReusableString> cacheRequestEncoder = new ReusableStringCacheRequestEncoder();
    private final Cluster cluster = Mockito.mock(Cluster.class);
    private static final long CLUSTER_TIME = 1_000L;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private AllTimersResult<ReusableString, ReusableString> result;
    private SBEDecodingCacheClusterService<ReusableString, ReusableString, ReusableString> sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        when(cluster.scheduleTimer(anyLong(), anyLong())).thenReturn(true);
        when(cluster.time()).thenReturn(CLUSTER_TIME);
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService<>("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.onStart(cluster, null);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new AllTimersResult<>();
    }

    @Test
    @DisplayName("Should return all scheduled removal timers")
    void shouldReturnScheduledTimers() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        ReusableString cacheId = ReusableString.build("testCacheId");
        ReusableString key = ReusableString.build("someKey★★★");
        ReusableString value = ReusableString.build("someValue★★★");
        long ttl = 10000;

        TestUtils.createCache(cacheId.value(), session, requestBuffer, sut);

        // add an entry with a ttl so that a removal timer is scheduled
        var addRequestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(addRequestId, cacheId, key, value, ttl, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        var requestId = UUID.randomUUID().toString();
        length = cacheRequestEncoder.encodeGetAllTimers(requestId, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeAllTimersResult(responseBuffer, 0, result);

        // Assert
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getOperationStatus());
        assertTrue(result.isEndOfBatch());

        var timers = result.getTimers();
        assertEquals(1, timers.size());

        TimerDetails<ReusableString, ReusableString> timer = timers.get(0);
        assertEquals(cacheId.value(), timer.getCacheId().value());
        assertEquals(key.value(), timer.getKey().value());
        assertEquals(TimerType.CACHE, timer.timerType);
        assertEquals(CLUSTER_TIME + ttl, timer.deadline);

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startGetAllTimersRequest(any(GetAllTimersRequestDetails.class));
        verify(tracingService, times(1)).endGetAllTimersRequest(any(GetAllTimersRequestDetails.class));
    }

    @Test
    @DisplayName("Should return an empty result when no timers are scheduled")
    void shouldReturnEmptyWhenNoTimers() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        ReusableString cacheId = ReusableString.build("testCacheId");

        TestUtils.createCache(cacheId.value(), session, requestBuffer, sut);

        // Act - request all timers without having scheduled any
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeGetAllTimers(requestId, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeAllTimersResult(responseBuffer, 0, result);

        // Assert
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getOperationStatus());
        assertTrue(result.getTimers().isEmpty());
    }
}
