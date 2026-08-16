package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.AddCacheEntryRequestDetails;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.Header;
import lombok.RequiredArgsConstructor;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test decoding an add cache entry request. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 *
 * @param <I> The cache ID type.
 * @param <K> The key type.
 * @param <V> The value type used by the encoder/decoder under test.
 * @param <FV> The value type of the CacheManagerFactory (may differ from V for counters).
 */
@RequiredArgsConstructor
public abstract class AbstractAddCacheEntryTest<I extends Reusable, K extends Reusable, V extends Reusable, FV extends Reusable> {

    private final Header header = new Header(0, 0);
    private final CacheManagerFactory<I, K, FV> cacheManagerFactory;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private AddCacheEntryResult<I, K> result;
    protected SBEDecodingCacheClusterService<I, K, FV> sut;
    private CacheTracingService tracingService;
    private final Cluster cluster = Mockito.mock(Cluster.class);

    @BeforeEach
    void setup() {
        when(cluster.scheduleTimer(anyLong(), anyLong())).thenReturn(true);
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService<>("node0", tracingService, cacheManagerFactory);
        sut.onStart(cluster, null);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new AddCacheEntryResult<>(cacheManagerFactory.getIndexSupplier().get(),
                cacheManagerFactory.getKeySupplier().get());
    }

    protected CacheSubscriptionService getSubscriptionServiceToVerify() {
        return sut.subscriptionService;
    }

    @Test
    @DisplayName("Should add entry to an existing cache")
    void shouldAddToKnownCache() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();
        V value = getValue();
        long ttl = 10000;

        createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeAddCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startAddCacheEntry(any(AddCacheEntryRequestDetails.class));
        verify(tracingService, times(1)).endAddCacheEntry(any(AddCacheEntryRequestDetails.class));

        // we schedule a timer for the ttl
        verify(cluster, times(1)).scheduleTimer(anyLong(), anyLong());

        // Call the subscription service on the back of adding an entry
        verify(getSubscriptionServiceToVerify(), times(1)).handleEntryAdded(
                any(AddCacheEntryResult.class),
                any(MutableDirectBuffer.class),
                eq(key),
                eq(value),
                anyInt());
    }

    @Test
    @DisplayName("Should reschedule ttl")
    void shouldRescheduleTTL() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getRescheduleCacheId();
        K key = getKey();
        V value = getValue();
        long ttl = 10000;

        createCache(cacheId, session, requestBuffer, sut);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        requestId = UUID.randomUUID().toString();
        ttl *= 2;
        length = encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        decodeAddCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
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
        I cacheId = getUnknownCacheId();
        K key = getKey();
        V value = getValue();
        long ttl = 0;

        // Act
        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeAddCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

    @Test
    @DisplayName("Should add entry to a dynamically created cache")
    void shouldAddToDynamicallyCreatedCache() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getDynamicCacheId();
        K key = getKey();
        V value = getValue();
        long ttl = 10000;

        sut.setDynamicCacheCreationEnabled(true);

        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeAddCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
    }

    public abstract int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, long ttl, MutableDirectBuffer buffer);

    public abstract void decodeAddCacheEntryResult(DirectBuffer buffer, int offset, AddCacheEntryResult<I, K> result);

    protected abstract void createCache(I cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<I, K, FV> sut);

    protected abstract I getCacheId();

    protected abstract I getRescheduleCacheId();

    protected abstract I getUnknownCacheId();

    protected abstract I getDynamicCacheId();

    protected abstract K getKey();

    protected abstract V getValue();
}
