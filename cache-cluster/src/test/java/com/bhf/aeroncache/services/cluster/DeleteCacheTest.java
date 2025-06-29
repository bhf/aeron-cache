package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.DeleteCacheRequestDetails;
import com.bhf.aeroncache.models.results.DeleteCacheResult;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a delete cache request.
 */
class DeleteCacheTest {

    private static final long MAX_SBE_LONG = Long.MAX_VALUE;
    private static final long MIN_SBE_LONG = -Long.MAX_VALUE;
    private final Header header = new Header(0, 0);
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final DeleteCacheEncoder deleteCacheEncoder = new DeleteCacheEncoder();
    private final CacheDeletedDecoder cacheDeletedDecoder = new CacheDeletedDecoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private DeleteCacheResult<ReusableLong> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new DeleteCacheResult<>(SupplierUtils.longSupplier.get());
    }

    @ParameterizedTest
    @DisplayName("Should return correct details of deleted cache")
    @ValueSource(longs = {0, MAX_SBE_LONG, MIN_SBE_LONG})
    @HappyPath
    void shouldDeleteKnownCache(long cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        TestUtils.createCache(cacheId, session, createCacheEncoder, headerEncoder, requestBuffer, sut, header);

        var requestId = UUID.randomUUID().toString();
        int length = CacheRequestEncoder.encodeDeleteCache(deleteCacheEncoder, headerEncoder,
                requestBuffer, requestId, cacheId);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeCacheDeleted(cacheDeletedDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.SUCCESS, result.getStatus());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startHandleDeleteCache(any(DeleteCacheRequestDetails.class));
        verify(tracingService, times(1)).endHandleDeleteCache(any(DeleteCacheRequestDetails.class));

        // Call the subscription service on the back of a delete cache
        verify(sut.subscriptionService, times(1)).handleDeleteCache(
                any(DeleteCacheResult.class),
                any(MutableDirectBuffer.class),
                any(CacheDeletedEncoder.class),
                any(MessageHeaderEncoder.class));
    }

    @Test
    @DisplayName("Should notify when cache is unknown")
    void shouldNotifyWhenCacheUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = 123L;
        var length = CacheRequestEncoder.encodeDeleteCache(deleteCacheEncoder, headerEncoder,
                requestBuffer, requestId, cacheId);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeCacheDeleted(cacheDeletedDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

}
