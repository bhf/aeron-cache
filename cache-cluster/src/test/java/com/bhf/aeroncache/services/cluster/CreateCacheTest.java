package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.CreateCacheRequestDetails;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.TestUtils;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a create cache request. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 */
class CreateCacheTest {

    private final Header header = new Header(0, 0);
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    private final CacheCreatedDecoder cacheCreatedDecoder = new CacheCreatedDecoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private CreateCacheResult<ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        var encoder = new ReusableStringCacheResponseEncoder();
        var decoder = new ReusableStringCacheRequestDecoder();
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory(),
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, encoder, decoder);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
    }

    /**
     * Test creating a cache.
     */
    @ParameterizedTest
    @DisplayName("Should return correct details of created cache")
    @ValueSource(strings = {"testCacheId"})
    void testCreateCacheMessage(String cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var length = CacheRequestEncoder.encodeCreateCacheRequest(createCacheEncoder, headerEncoder,
                requestBuffer, requestId, cacheId);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeCacheCreated(result, cacheCreatedDecoder, headerDecoder, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.SUCCESS, result.getStatus());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startCreateCacheRequest(any(CreateCacheRequestDetails.class));
        verify(tracingService, times(1)).endCreateCacheRequest(any(CreateCacheRequestDetails.class));
    }

    @Test
    @DisplayName("Should notify when cache already exists")
    void shouldNotifyWhenCacheExists() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var length = CacheRequestEncoder.encodeCreateCacheRequest(createCacheEncoder, headerEncoder,
                requestBuffer, requestId, cacheId);
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);

        // Act
        length = CacheRequestEncoder.encodeCreateCacheRequest(createCacheEncoder, headerEncoder,
                requestBuffer, requestId, cacheId);
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeCacheCreated(result, cacheCreatedDecoder, headerDecoder, responseBuffer, 0);

        // Assert
        assertEquals(OperationStatus.CACHE_EXISTS, result.getStatus());
    }

}
