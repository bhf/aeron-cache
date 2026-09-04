package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.ClearCacheRequestDetails;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.ClearCacheResult;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a clear cache request. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 */
@RequiredArgsConstructor
public abstract class AbstractClearCacheTest<I extends Reusable, K extends Reusable, V extends Reusable> {

    private final Header header = new Header(0, 0);
    private final CacheManagerFactory<I, K, V> cacheManagerFactory;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private ClearCacheResult<I> result;
    protected SBEDecodingCacheClusterService<I, K, V> sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService<>("node0", tracingService, cacheManagerFactory);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new ClearCacheResult<>(cacheManagerFactory.getIndexSupplier().get());
    }

    protected CacheSubscriptionService getSubscriptionServiceToVerify() {
        return sut.subscriptionService;
    }

    @Test
    @DisplayName("Should return correct details of cleared cache")
    void shouldClearKnownCache() {
        List<I> cacheIds = getCacheIdsToClear();
        for (I cacheId : cacheIds) {
            processClearMessage(cacheId);
        }

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(cacheIds.size())).startHandleClearCache(any(ClearCacheRequestDetails.class));
        verify(tracingService, times(cacheIds.size())).endHandleClearCache(any(ClearCacheRequestDetails.class));

        // Call the subscription service on the back of a clear cache
        verify(getSubscriptionServiceToVerify(), times(cacheIds.size())).handleClearCache(
                any(ClearCacheResult.class),
                any(MutableDirectBuffer.class),
                anyInt(),
                anyLong());
    }

    protected abstract List<I> getCacheIdsToClear();

    void processClearMessage(I cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        createCache(cacheId, session, requestBuffer, sut);

        var requestId = UUID.randomUUID().toString();
        int length = encodeClearCache(requestId, cacheId, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        decodeCacheCleared(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
    }

    public abstract int encodeClearCache(String requestId, I cacheId, MutableDirectBuffer buffer);

    public abstract void decodeCacheCleared(DirectBuffer buffer, int offset, ClearCacheResult<I> result);

    protected abstract void createCache(I cacheId, ClientSession session, MutableDirectBuffer requestBuffer, SBEDecodingCacheClusterService<I, K, V> sut);

    @Test
    @DisplayName("Should notify when cache is unknown")
    void shouldNotifyWhenCacheUnknown() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = getUnknownCacheId();
        var length = encodeClearCache(requestId, cacheId, requestBuffer);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        decodeCacheCleared(responseBuffer, 0, result);

        // Assert
        assertEquals(cacheId.value(), result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.UNKNOWN_CACHE, result.getStatus());
    }

    protected abstract I getUnknownCacheId();
}
