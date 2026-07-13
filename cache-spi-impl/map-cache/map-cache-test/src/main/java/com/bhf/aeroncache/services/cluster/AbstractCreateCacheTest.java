package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.application.TestUtils;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.CreateCacheRequestDetails;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test decoding a create cache request. Uses a Detroit style for simplicity in
 * decoding the response buffer.
 */
@RequiredArgsConstructor
public abstract class AbstractCreateCacheTest<I extends Reusable, K extends Reusable, V extends Reusable> {

    private final Header header = new Header(0, 0);
    private final CacheManagerFactory<I,K,V> cacheManagerFactory;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private CreateCacheResult<I> result;
    private SBEDecodingCacheClusterService<I,K,V> sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, cacheManagerFactory);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new CreateCacheResult<>(cacheManagerFactory.getIndexSupplier().get());
    }

    /**
     * Test creating a cache.
     */
    @Test
    @DisplayName("Should return correct details of created cache")
    void testCreateMessage() {
        List<I> cacheIds = getCacheIdsToCreate();
        for(I c: cacheIds){
            processCreateMessage(c);
        }

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(cacheIds.size())).startCreateCacheRequest(any(CreateCacheRequestDetails.class));
        verify(tracingService, times(cacheIds.size())).endCreateCacheRequest(any(CreateCacheRequestDetails.class));
    }

    protected abstract List<I> getCacheIdsToCreate();


    void processCreateMessage(I cacheId) {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var length = encodeCreate(cacheId, requestId, requestBuffer);

        // Act
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        decodeCreate(responseBuffer, result);

        // Assert
        assertEquals(cacheId, result.getCacheId().value());
        assertEquals(requestId, result.getRequestId());
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());

    }

    public abstract void decodeCreate(DirectBuffer responseBuffer_, CreateCacheResult<I> result_);

    public abstract int encodeCreate(I cacheId, String requestId, MutableDirectBuffer requestBuffer_);

    @Test
    @DisplayName("Should notify when cache already exists")
    void shouldNotifyWhenCacheExists() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);
        var requestId = UUID.randomUUID().toString();
        var cacheId = getDuplicateCacheId();
        var length = encodeCreate(cacheId, requestId, requestBuffer);
        long ts = System.currentTimeMillis();
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);

        // Act
        length = encodeCreate(cacheId, requestId, requestBuffer);
        sut.onSessionMessage(session, ts, requestBuffer, 0, length, header);
        decodeCreate(responseBuffer, result);

        // Assert
        assertEquals(CacheOperationStatus.CACHE_EXISTS, result.getStatus());
    }

    protected abstract I getDuplicateCacheId();

}
