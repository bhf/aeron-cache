package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CacheSubscriptionResult;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import lombok.RequiredArgsConstructor;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a subscribe to cache request. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 *
 * @param <I>  The cache ID type.
 * @param <K>  The key type.
 * @param <V>  The value type used by the encoder/decoder under test.
 * @param <FV> The value type of the CacheManagerFactory.
 */
@RequiredArgsConstructor
public abstract class AbstractSubscribeCacheTest<I extends Reusable, K extends Reusable, V extends Reusable, FV extends Reusable> {

    private final Header header = new Header(0, 0);
    private final CacheManagerFactory<I, K, FV> cacheManagerFactory;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private CacheSubscriptionResult<I, K, V> result;
    private SBEDecodingCacheClusterService<I, K, FV> sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService<>("node0", tracingService, cacheManagerFactory);
        IdleStrategy idleStrategy = Mockito.mock(IdleStrategy.class);
        setupSubscriptionService(sut, idleStrategy);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = createResult();
    }

    @Test
    @DisplayName("Should subscribe to a known cache")
    void shouldSubscribeToKnownCache() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();

        createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();
        int length = encodeCacheSubscribe(requestId, List.of(cacheId), false, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeCacheSubscribeResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());

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
        I cacheId = getUnknownCacheId();
        var length = encodeCacheSubscribe(requestId, List.of(cacheId), false, requestBuffer);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        decodeCacheSubscribeResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("Should subscribe with hydration")
    void shouldSubscribeWithHydration() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getHydrationCacheId();

        createCache(cacheId, session, requestBuffer, sut);

        K key = getHydrationKey();
        V value = getHydrationValue();
        var addLength = encodeAddCacheEntry(UUID.randomUUID().toString(), cacheId, key, value, 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, addLength, header);

        var requestId = UUID.randomUUID().toString();
        int subscribeLength = encodeCacheSubscribe(requestId, List.of(cacheId), true, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, subscribeLength, header);
        decodeCacheSubscribeResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());

        var entries = result.getEntries();
        assertEquals(1, entries.size());
        assertTrue(entries.containsKey(key));
        assertEquals(value, entries.get(key));
    }

    protected abstract CacheSubscriptionResult<I, K, V> createResult();

    protected abstract void setupSubscriptionService(SBEDecodingCacheClusterService<I, K, FV> sut, IdleStrategy idleStrategy);

    public abstract int encodeCacheSubscribe(String requestId, List<I> cacheIds, boolean sendSnapshot, MutableDirectBuffer buffer);

    public abstract int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, long ttl, MutableDirectBuffer buffer);

    public abstract void decodeCacheSubscribeResult(DirectBuffer buffer, int offset, CacheSubscriptionResult<I, K, V> result);

    protected abstract void createCache(I cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<I, K, FV> sut);

    protected abstract I getCacheId();

    protected abstract I getUnknownCacheId();

    protected abstract I getHydrationCacheId();

    protected abstract K getHydrationKey();

    protected abstract V getHydrationValue();
}
