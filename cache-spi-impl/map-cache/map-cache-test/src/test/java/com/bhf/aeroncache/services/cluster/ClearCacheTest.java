package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.request.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.requests.ClearCacheRequestDetails;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import org.agrona.AbstractMutableDirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a clear cache request.
 */
class ClearCacheTest {

    private final Header header = new Header(0, 0);
    private final CacheResponseDecoder cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    private final CacheRequestEncoder cacheRequestEncoder = new RegularStringCacheRequestEncoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private ClearCacheResult<ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new ClearCacheResult<>(SupplierUtils.stringSupplier.get());
    }

    @ParameterizedTest
    @DisplayName("Should return correct details of cleared cache")
    @ValueSource(strings = {"testCacheId"})
    @HappyPath
    void shouldClearKnownCache(String cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        TestUtils.createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();
        int length = cacheRequestEncoder.encodeClearCache(requestId, cacheId, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeCacheCleared(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startHandleClearCache(any(ClearCacheRequestDetails.class));
        verify(tracingService, times(1)).endHandleClearCache(any(ClearCacheRequestDetails.class));

        verify(sut.subscriptionService, times(1)).handleClearCache(
                any(ClearCacheResult.class),
                any(AbstractMutableDirectBuffer.class),
                anyInt(),
                eq(session.id()));
    }

    @Test
    @DisplayName("Should notify when cache is unknown")
    void shouldNotifyWhenCacheUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var length = cacheRequestEncoder.encodeClearCache(requestId, cacheId, requestBuffer);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeCacheCleared(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

}
