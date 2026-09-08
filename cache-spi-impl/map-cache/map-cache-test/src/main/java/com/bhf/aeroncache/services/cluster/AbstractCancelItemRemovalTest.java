package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CancelItemRemovalResult;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test decoding a cancel item removal request. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 *
 * @param <I>  The cache ID type.
 * @param <K>  The key type.
 * @param <V>  The value type used by the encoder/decoder under test.
 * @param <FV> The value type of the CacheManagerFactory.
 */
@RequiredArgsConstructor
public abstract class AbstractCancelItemRemovalTest<I extends Reusable, K extends Reusable, V extends Reusable, FV extends Reusable> {

    private final Header header = new Header(0, 0);
    private final CacheManagerFactory<I, K, FV> cacheManagerFactory;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private CancelItemRemovalResult<I, K> result;
    protected SBEDecodingCacheClusterService<I, K, FV> sut;
    private CacheTracingService tracingService;
    private final Cluster cluster = Mockito.mock(Cluster.class);

    @BeforeEach
    void setup() {
        when(cluster.scheduleTimer(anyLong(), anyLong())).thenReturn(true);
        when(cluster.cancelTimer(anyLong())).thenReturn(true);
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService<>("node0", tracingService, cacheManagerFactory);
        sut.onStart(cluster, null);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = createResult();
    }

    @Test
    @DisplayName("Should cancel a scheduled item removal")
    void shouldCancelScheduledItemRemoval() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();
        V value = getValue();
        long ttl = 10000;

        createCache(cacheId, session, requestBuffer, sut);

        // add an entry to the cache with a ttl so that a removal timer is scheduled
        var requestId = UUID.randomUUID().toString();
        var length = encodeAddCacheEntry(requestId, cacheId, key, value, ttl, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);

        // Act
        requestId = UUID.randomUUID().toString();
        length = encodeCancelItemRemoval(requestId, cacheId, key, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeItemRemovalCancelled(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(key.value(), result.getKey().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertTrue(result.isCancelled());

        // the scheduled removal timer was cancelled
        verify(cluster, times(1)).cancelTimer(anyLong());
    }

    @Test
    @DisplayName("Should not cancel when no removal was scheduled")
    void shouldNotCancelWhenNoTimerScheduled() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        I cacheId = getCacheId();
        K key = getKey();

        createCache(cacheId, session, requestBuffer, sut);

        // Act - cancel a removal that was never scheduled
        var requestId = UUID.randomUUID().toString();
        var length = encodeCancelItemRemoval(requestId, cacheId, key, requestBuffer);
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeItemRemovalCancelled(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, result.getStatus());
        assertFalse(result.isCancelled());

        // no timer was cancelled since none was scheduled
        verify(cluster, never()).cancelTimer(anyLong());
    }

    protected abstract CancelItemRemovalResult<I, K> createResult();

    public abstract int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, long ttl, MutableDirectBuffer buffer);

    public abstract int encodeCancelItemRemoval(String requestId, I cacheId, K key, MutableDirectBuffer buffer);

    public abstract void decodeItemRemovalCancelled(DirectBuffer buffer, int offset, CancelItemRemovalResult<I, K> result);

    protected abstract void createCache(I cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<I, K, FV> sut);

    protected abstract I getCacheId();

    protected abstract K getKey();

    protected abstract V getValue();
}
