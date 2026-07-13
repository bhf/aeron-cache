package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.GetAllCacheEntriesRequestDetails;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.GetAllCacheEntriesResult;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a request to get all entries from a cache. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 *
 * @param <I> The cache ID type.
 * @param <K> The key type.
 * @param <V> The value type used by the encoder/decoder under test.
 * @param <FV> The value type of the CacheManagerFactory.
 */
@RequiredArgsConstructor
public abstract class AbstractGetCacheEntriesTest<I extends Reusable, K extends Reusable, V extends Reusable, FV extends Reusable> {

    private final Header header = new Header(0, 0);
    private final CacheManagerFactory<I, K, FV> cacheManagerFactory;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private GetAllCacheEntriesResult<I, K, V> result;
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
    @DisplayName("Should return entries from a known cache")
    void shouldGetEntriesFromKnownCache() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();

        createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();

        // Add some items into the cache
        List<KeyValue<K, V>> entries = getEntriesToAdd();
        for (var entry : entries) {
            var length = encodeAddCacheEntry(requestId, cacheId, entry.key(), entry.value(), 0, requestBuffer);
            sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        }

        int length = encodeGetCacheEntries(requestId, cacheId, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeAllCacheEntriesResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());

        var returnedCachedEntries = result.value().getValues();
        for (var entry : entries) {
            assertTrue(returnedCachedEntries.containsKey(entry.key()));
            assertEquals(entry.value(), returnedCachedEntries.get(entry.key()));
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
        I cacheId = getUnknownCacheId();
        var length = encodeGetCacheEntries(requestId, cacheId, requestBuffer);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        decodeAllCacheEntriesResult(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

    protected abstract GetAllCacheEntriesResult<I, K, V> createResult();

    public abstract int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, long ttl, MutableDirectBuffer buffer);

    public abstract int encodeGetCacheEntries(String requestId, I cacheId, MutableDirectBuffer buffer);

    public abstract void decodeAllCacheEntriesResult(DirectBuffer buffer, int offset, GetAllCacheEntriesResult<I, K, V> result);

    protected abstract void createCache(I cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<I, K, FV> sut);

    protected abstract I getCacheId();

    protected abstract I getUnknownCacheId();

    protected abstract List<KeyValue<K, V>> getEntriesToAdd();

    public record KeyValue<K, V>(K key, V value) {}
}
