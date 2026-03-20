package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.AddCacheEntryRequestDetails;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding an add cache entry request.
 */
class AddCacheEntryTest {

    private final Header header = new Header(0, 0);
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private AddCacheEntryResult<ReusableString, ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private final CacheRequestEncoder cacheRequestEncoder = new CacheRequestEncoder();
    private final AddCacheEntryEncoder addCacheEntryEncoder = new AddCacheEntryEncoder();
    private final CacheEntryCreatedDecoder cacheEntryCreatedDecoder = new CacheEntryCreatedDecoder();

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new AddCacheEntryResult<>(SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get());
    }

    @ParameterizedTest
    @DisplayName("Should add entry to an existing cache")
    @ValueSource(strings = {"testCacheId"})
    @HappyPath
    void shouldAddToKnownCache(String cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var key = "someKey";
        var value = "someValue";

        // create the cache
        TestUtils.createCache(cacheId, session, requestBuffer, sut);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeAddCacheEntryResult(cacheEntryCreatedDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.SUCCESS, result.getStatus());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startAddCacheEntry(any(AddCacheEntryRequestDetails.class));
        verify(tracingService, times(1)).endAddCacheEntry(any(AddCacheEntryRequestDetails.class));

        // Call the subscription service on the back of adding an entry
        var reusableKey = new ReusableString();
        reusableKey.copyFrom(key);

        var reusableValue = new ReusableString();
        reusableValue.copyFrom(value);

        verify(sut.subscriptionService, times(1)).handleEntryAdded(
                any(AddCacheEntryResult.class),
                any(MutableDirectBuffer.class),
                eq(reusableKey),
                eq(reusableValue),
                anyInt());
    }

    @Test
    @DisplayName("Should notify when cache is unknown")
    void shouldNotifyWhenCacheIsUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var key = "someKey";
        var value = "someValue";

        // Act
        requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeAddCacheEntryResult(cacheEntryCreatedDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

}
