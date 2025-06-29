package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import com.bhf.aeroncache.models.results.CacheSubscriptionResult;
import com.bhf.aeroncache.models.results.CacheUnsubscribeResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionServiceImpl;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a subscribe to cache request.
 */
class SubscribeCacheTest {

    private static final long MAX_SBE_LONG = Long.MAX_VALUE;
    private static final long MIN_SBE_LONG = -Long.MAX_VALUE;
    private final Header header = new Header(0, 0);
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CacheSubscriptionRequestEncoder subscribeCacheEncoder = new CacheSubscriptionRequestEncoder();
    private final CacheSubscriptionResponseDecoder cacheSubscribedDecoder = new CacheSubscriptionResponseDecoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private CacheSubscriptionResult<ReusableLong> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService);
        IdleStrategy idleStrategy = Mockito.mock(IdleStrategy.class);
        CacheSubscriptionResult<ReusableLong> subscriptionResult = new CacheSubscriptionResult<>(new ReusableLong());
        CacheUnsubscribeResult<ReusableLong> unsubscribeResult = new CacheUnsubscribeResult<>(new ReusableLong());
        Supplier<ReusableLong> indexSupplier = ReusableLong::new;
        sut.subscriptionService = new CacheSubscriptionServiceImpl<>(idleStrategy, subscriptionResult, unsubscribeResult, indexSupplier);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new CacheSubscriptionResult<>(SupplierUtils.longSupplier.get());
    }

    @ParameterizedTest
    @DisplayName("Should subscribe to a known cache")
    @ValueSource(longs = {0, MAX_SBE_LONG, MIN_SBE_LONG})
    @HappyPath
    void shouldSubscribeToKnownCache(long cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        TestUtils.createCache(cacheId, session, createCacheEncoder, headerEncoder, requestBuffer, sut, header);

        var requestId = UUID.randomUUID().toString();
        int length = CacheRequestEncoder.encodeCacheSubscribe(subscribeCacheEncoder, headerEncoder,
                requestBuffer, requestId, cacheId);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeCacheSubscribeResult(cacheSubscribedDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.SUCCESS, result.getStatus());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startCacheSubscriptionRequest(any(CacheSubscriptionRequestDetails.class));
        verify(tracingService, times(1)).endCacheSubscriptionRequest(any(CacheSubscriptionRequestDetails.class));
    }

    @Test
    @DisplayName("Should allow subscription to an unknown cache")
    void shouldNotifyWhenCacheUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = 123L;
        var length = CacheRequestEncoder.encodeCacheSubscribe(subscribeCacheEncoder, headerEncoder,
                requestBuffer, requestId, cacheId);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeCacheSubscribeResult(cacheSubscribedDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.SUCCESS, result.getStatus());
    }

}
