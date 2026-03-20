package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.AllCacheEntriesResultDecoder;
import com.bhf.aeroncache.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.requests.GetAllCacheEntriesRequestDetails;
import com.bhf.aeroncache.models.results.GetAllCacheEntriesResult;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a request to get all entries from a cache.
 */
class GetCacheEntriesTest {

    private final Header header = new Header(0, 0);
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final AllCacheEntriesResultDecoder allCacheEntriesResultDecoder = new AllCacheEntriesResultDecoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private final CacheRequestEncoder cacheRequestEncoder = new CacheRequestEncoder();

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new GetAllCacheEntriesResult<>(SupplierUtils.stringSupplier.get());
    }

    @ParameterizedTest
    @DisplayName("Should return entries from a known cache")
    @ValueSource(strings = {"testCacheId"})
    @HappyPath
    void shouldGetEntriesFromKnownCache(String cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        TestUtils.createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();

        // Add some items into the cache
        int itemsToAdd = 10;
        for (int i = 0; i < itemsToAdd; i++) {
            var key = "key-"+i;
            var value = "value-"+i;

            var length = cacheRequestEncoder.encodeAddCacheEntry(requestBuffer, requestId, cacheId, key, value);
            sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        }

        int length = cacheRequestEncoder.encodeGetCacheEntries(requestBuffer, requestId, cacheId);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeAllCacheEntriesResult(allCacheEntriesResultDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.SUCCESS, result.getStatus());

        var returnedCachedEntries = result.value().getValues();

        for (int i = 0; i < itemsToAdd; i++) {
            var expectedKey = new ReusableString();
            expectedKey.copyFrom("key-" + i);
            var expectedValue = new ReusableString();
            expectedValue.copyFrom("value-" + i);
            assertTrue(returnedCachedEntries.containsKey(expectedKey));
            assertEquals(expectedValue, returnedCachedEntries.get(expectedKey));
        }

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startGetAllCacheEntries(any(GetAllCacheEntriesRequestDetails.class));
        verify(tracingService, times(1)).endGetAllCacheEntries(any(GetAllCacheEntriesRequestDetails.class));
    }

    @Test
    @DisplayName("Should notify when cache is unknown")
    void shouldNotifyWhenCacheUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var length = cacheRequestEncoder.encodeGetCacheEntries(requestBuffer, requestId, cacheId);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeAllCacheEntriesResult(allCacheEntriesResultDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

}
