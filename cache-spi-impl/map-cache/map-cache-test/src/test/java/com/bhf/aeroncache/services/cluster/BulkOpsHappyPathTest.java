package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.bhf.aeroncache.models.requests.BulkCacheOpsRequestDetails;
import com.bhf.aeroncache.models.results.*;
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
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static com.bhf.aeroncache.services.TestUtils.assertRequestIdCacheIdMatch;
import static com.bhf.aeroncache.services.TestUtils.getCacheOperation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test bulk operations on a cache instance.
 */
@HappyPath
class BulkOpsHappyPathTest {

    public static final String CACHE_ID = "bulk";
    private final Header header = new Header(0, 0);
    private final CacheResponseDecoder cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    private final CacheRequestEncoder cacheRequestEncoder = new RegularStringCacheRequestEncoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private BulkCacheOpsResult<ReusableString,ReusableString,ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier,SupplierUtils.stringSupplier,SupplierUtils.stringSupplier);
    }

    @Test
    @DisplayName("Should handle bulk operations")
    @HappyPath
    void shouldHandleBulkOperation() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);

        var requestId = UUID.randomUUID().toString();
        List<CacheOperationRequest> ops = List.of(
                getCacheOperation(BulkOperationType.CREATE_CACHE, CACHE_ID, "", "", 0),
                getCacheOperation(BulkOperationType.ADD_ITEM, CACHE_ID, "key", "value", 0),
                getCacheOperation(BulkOperationType.GET_ITEM, CACHE_ID, "key", "", 0),
                getCacheOperation(BulkOperationType.REMOVE_ITEM, CACHE_ID, "key", "", 0),
                getCacheOperation(BulkOperationType.CLEAR_CACHE, CACHE_ID, "", "", 0),
                getCacheOperation(BulkOperationType.CREATE_CACHE, CACHE_ID, "", "", 0),
                getCacheOperation(BulkOperationType.GET_ITEM, CACHE_ID, "key", "", 0),
                getCacheOperation(BulkOperationType.DELETE_CACHE, CACHE_ID, "", "", 0)
        );

        BulkCacheOpsRequest bulkRequest = new BulkCacheOpsRequest(requestId, ops);
        int length = cacheRequestEncoder.encodeBulkOperations(requestId, bulkRequest, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeBulkCacheOpsResult(responseBuffer, 0, result);
        var opResults = result.getOperations();

        // Assert
        assertEquals(requestId, result.getRequestId());
        assertEquals(8, opResults.size());

        // Create cache assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(0).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 0);

        // Add item assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(1).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 1);
        verify(sut.subscriptionService, times(1)).handleEntryAdded(
                any(AddCacheEntryResult.class),
                any(AbstractMutableDirectBuffer.class),
                any(ReusableString.class),
                any(ReusableString.class),
                anyInt());

        // Get item assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(2).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 2);
        assertEquals("value", opResults.get(2).getValue().value());

        // Remove item assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(3).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 3);
        verify(sut.subscriptionService, times(1)).handleEntryRemoved(
                any(RemoveCacheEntryResult.class),
                any(AbstractMutableDirectBuffer.class),
                anyInt(), anyLong());

        // Clear cache assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(4).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 4);
        verify(sut.subscriptionService, times(1)).handleClearCache(
                any(ClearCacheResult.class),
                any(AbstractMutableDirectBuffer.class),
                anyInt(), anyLong());

        // Duplicate create cache assertions
        assertEquals(CacheOperationStatus.CACHE_EXISTS, opResults.get(5).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 5);

        // Get unknown item assertions
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, opResults.get(6).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 6);

        // Delete cache assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(7).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 7);
        verify(sut.subscriptionService, times(1)).handleDeleteCache(
                any(DeleteCacheResult.class),
                any(AbstractMutableDirectBuffer.class),
                anyInt(), anyLong());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startBulkOpsRequest(any(BulkCacheOpsRequestDetails.class));
        verify(tracingService, times(1)).endBulkOpsRequest(any(BulkCacheOpsRequestDetails.class));
    }


}
