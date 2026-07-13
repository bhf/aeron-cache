package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.GetCacheEntryRequestDetails;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import io.aeron.cluster.service.ClientSession;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a get cache entry request. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 *
 * @param <I> The cache ID type.
 * @param <K> The key type.
 * @param <V> The value type used by the encoder/decoder under test.
 * @param <FV> The value type of the CacheManagerFactory.
 */
@RequiredArgsConstructor
public abstract class AbstractGetCacheEntryTest<I extends Reusable, K extends Reusable, V extends Reusable, FV extends Reusable> {

    private final Header header = new Header(0, 0);
    private final CacheManagerFactory<I, K, FV> cacheManagerFactory;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private GetCacheEntryResult<I, K, V> result;
    protected SBEDecodingCacheClusterService<I, K, FV> sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService<>("node0", tracingService, cacheManagerFactory);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = createResult();
    }

    @Test
    @DisplayName("Should return existing cache entry")
    void shouldReturnKnownEntry() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();
        V value = getValue();

        createCache(cacheId, session, requestBuffer, sut);

        // add an entry to the cache
        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, 0, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        length = encodeGetCacheEntry(requestId, cacheId, key, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeGetCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals(key.value(), result.getEntryKey().value());
        assertEquals(value.value(), result.getEntryValue().value());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startGetCacheEntry(any(GetCacheEntryRequestDetails.class));
        verify(tracingService, times(1)).endGetCacheEntry(any(GetCacheEntryRequestDetails.class));
    }

    @Test
    @DisplayName("Should notify when entry key is unknown")
    void shouldNotifyWhenEntryKeyIsUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getUnknownKeyCacheId();
        K key = getKey();

        createCache(cacheId, session, requestBuffer, sut);

        // Act
        var requestId = UUID.randomUUID().toString();
        var length = encodeGetCacheEntry(requestId, cacheId, key, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeGetCacheEntryResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, result.getStatus());
    }

    protected abstract GetCacheEntryResult<I, K, V> createResult();

    public abstract int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, long ttl, MutableDirectBuffer buffer);

    public abstract int encodeGetCacheEntry(String requestId, I cacheId, K key, MutableDirectBuffer buffer);

    public abstract void decodeGetCacheEntryResult(DirectBuffer buffer, int offset, GetCacheEntryResult<I, K, V> result);

    protected abstract void createCache(I cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<I, K, FV> sut);

    protected abstract I getCacheId();

    protected abstract I getUnknownKeyCacheId();

    protected abstract K getKey();

    protected abstract V getValue();
}
