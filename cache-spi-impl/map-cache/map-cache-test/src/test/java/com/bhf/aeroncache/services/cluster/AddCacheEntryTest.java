package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.request.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.requests.AddCacheEntryRequestDetails;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.Header;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import com.bhf.aeroncache.services.TestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test decoding an add cache entry request.
 */
class AddCacheEntryTest {

    private final Header header = new Header(0, 0);
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private AddCacheEntryResult<ReusableString, ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private final CacheRequestEncoder cacheRequestEncoder = new RegularStringCacheRequestEncoder();
    private final CacheResponseDecoder cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    private final Cluster cluster = Mockito.mock(Cluster.class);

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.onStart(cluster, null);

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
        var key = "someKey★★★";
        var value = "someValue★★★";
        var ttl = 10000;

        // create the cache
        TestUtils.createCache(cacheId, session, requestBuffer, sut);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeAddCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startAddCacheEntry(any(AddCacheEntryRequestDetails.class));
        verify(tracingService, times(1)).endAddCacheEntry(any(AddCacheEntryRequestDetails.class));

        // we schedule a timer for the ttl
        verify(cluster, times(1)).scheduleTimer(anyLong(), anyLong());

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

    @ParameterizedTest
    @DisplayName("Should reschedule ttl")
    @ValueSource(strings = {"testCacheId-ttl-resched"})
    @HappyPath
    void shouldRescheduleTTL(String cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var key = "someKey★★★";
        var value = "someValue★★★";
        var ttl = 10000;

        // create the cache
        TestUtils.createCache(cacheId, session, requestBuffer, sut);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        requestId = UUID.randomUUID().toString();
        ttl*=2;
        length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        cacheResponseDecoder.decodeAddCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());


        // we scheduled a timer for the ttl and rescheduled it
        verify(cluster, times(2)).scheduleTimer(anyLong(), anyLong());

        // we cancelled the timer before we rescheduled it
        verify(cluster, times(1)).cancelTimer(anyLong());

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
        var ttl = 0;

        // Act
        requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeAddCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

    @ParameterizedTest
    @DisplayName("Should add entry to a dynamically created cache")
    @ValueSource(strings = {"Some-Uncreated-Cache"})
    @HappyPath
    void shouldAddToDynamicallyCreatedCache(String cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var key = "someKey★★★";
        var value = "someValue★★★";
        var ttl = 10000;

        sut.setDynamicCacheCreationEnabled(true);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeAddCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());

    }

}
