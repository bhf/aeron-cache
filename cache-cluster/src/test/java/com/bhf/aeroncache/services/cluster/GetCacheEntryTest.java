package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.GetCacheEntryRequestDetails;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.services.TestUtils;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a get cache entry request.
 */
class GetCacheEntryTest {

    private final Header header = new Header(0, 0);
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private GetCacheEntryResult<ReusableString, ReusableString, ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private final CacheRequestEncoder cacheRequestEncoder = new CacheRequestEncoder();
    private final CacheEntryResultDecoder cacheEntryResultDecoder = new CacheEntryResultDecoder();

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new GetCacheEntryResult<>(SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get());
    }

    @ParameterizedTest
    @DisplayName("Should return existing cache entry")
    @ValueSource(strings = {"testCacheId"})
    @HappyPath
    void shouldReturnKnownEntry(String cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var key = "someKey";
        var value = "someValue";

        // create the cache
        TestUtils.createCache(cacheId, session, requestBuffer, sut);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        length = cacheRequestEncoder.encodeGetCacheEntry(requestId, cacheId, key, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeGetCacheEntryResult(cacheEntryResultDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.SUCCESS, result.getStatus());
        assertEquals(result.getEntryKey().value(), key);
        assertEquals(result.getEntryValue().value(), value);

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startGetCacheEntry(any(GetCacheEntryRequestDetails.class));
        verify(tracingService, times(1)).endGetCacheEntry(any(GetCacheEntryRequestDetails.class));
    }

    @Test
    @DisplayName("Should notify when entry key is unknown")
    void shouldNotifyWhenEntryKeyIsUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var key = "someKey";
        TestUtils.createCache(cacheId, session, requestBuffer, sut);

        // Act
        requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeGetCacheEntry(requestId, cacheId, key, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeGetCacheEntryResult(cacheEntryResultDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.UNKNOWN_KEY, result.getStatus());
    }

}
