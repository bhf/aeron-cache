package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.RemoveCacheEntryRequestDetails;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a remove cache entry request.
 */
class RemoveCacheEntryTest {

    private final Header header = new Header(0, 0);
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private RemoveCacheEntryResult<ReusableString, ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    private final RemoveCacheEntryEncoder removeCacheEntryEncoder = new RemoveCacheEntryEncoder();
    private final AddCacheEntryEncoder addCacheEntryEncoder = new AddCacheEntryEncoder();
    private final CacheEntryRemovedDecoder cacheEntryRemovedDecoder = new CacheEntryRemovedDecoder();

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new RemoveCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
    }

    @ParameterizedTest
    @DisplayName("Should return correct details of removing a cache entry")
    @ValueSource(strings = {"testCacheId"})
    @HappyPath
    void shouldRemoveKnownCache(String cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var key = "someKey";
        var value = "someValue";

        // create the cache
        TestUtils.createCache(cacheId, session, createCacheEncoder, headerEncoder, requestBuffer, sut, header);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = CacheRequestEncoder.encodeAddCacheEntry(addCacheEntryEncoder, headerEncoder,
                requestBuffer, requestId, cacheId, key, value);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        length = CacheRequestEncoder.encodeRemoveCacheEntry(removeCacheEntryEncoder, headerEncoder,
                requestBuffer, requestId, cacheId, key);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeCacheEntryRemoved(cacheEntryRemovedDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.SUCCESS, result.getStatus());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startRemoveCacheEntry(any(RemoveCacheEntryRequestDetails.class));
        verify(tracingService, times(1)).endRemoveCacheEntry(any(RemoveCacheEntryRequestDetails.class));

        verify(sut.subscriptionService, times(1)).handleEntryRemoved(
                any(RemoveCacheEntryResult.class),
                any(AbstractMutableDirectBuffer.class),
                any(CacheEntryRemovedEncoder.class),
                any(MessageHeaderEncoder.class),
                eq(session.id()));
    }

    @Test
    @DisplayName("Should notify when entry key is unknown")
    void shouldNotifyWhenEntryKeyIsUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var key = "someKey";
        TestUtils.createCache(cacheId, session, createCacheEncoder, headerEncoder, requestBuffer, sut, header);

        // Act
        requestId = UUID.randomUUID().toString();
        var length = CacheRequestEncoder.encodeRemoveCacheEntry(removeCacheEntryEncoder, headerEncoder,
                requestBuffer, requestId, cacheId, key);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        CacheResponseDecoder.decodeCacheEntryRemoved(cacheEntryRemovedDecoder, headerDecoder, result, responseBuffer, 0);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(OperationStatus.UNKNOWN_KEY, result.getStatus());
    }

}
