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
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.BulkCacheOpsResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test bulk ops which are formed of multiple add and get item operations on the same keys
 * on the same cache.
 */
class BulkOpsAddGetOrderingCacheTest {

    public static final String CACHE_ID = "bulk-multi-ordered";
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
        result = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
    }

    @Test
    @DisplayName("Should handle multiple bulk add and get item operations in order")
    @HappyPath
    void shouldHandleMultiAddBulkOperationInOrder() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);

        var requestId = UUID.randomUUID().toString();
        List<CacheOperationRequest> ops = List.of(
            getCacheOperation(BulkOperationType.CREATE_CACHE, CACHE_ID, "", "", 0),

            getCacheOperation(BulkOperationType.ADD_ITEM, CACHE_ID, "key", "value1", 0),
            getCacheOperation(BulkOperationType.GET_ITEM, CACHE_ID, "key", "", 0),

            getCacheOperation(BulkOperationType.ADD_ITEM, CACHE_ID, "key", "value2", 0),
            getCacheOperation(BulkOperationType.GET_ITEM, CACHE_ID, "key", "", 0),

            getCacheOperation(BulkOperationType.ADD_ITEM, CACHE_ID, "key", "value3", 0),
            getCacheOperation(BulkOperationType.GET_ITEM, CACHE_ID, "key", "", 0)
        );

        BulkCacheOpsRequest bulkRequest = new BulkCacheOpsRequest(requestId, ops);
        int length = cacheRequestEncoder.encodeBulkOperations(requestId, bulkRequest, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeBulkCacheOpsResult(responseBuffer, 0, result);
        var opResults = result.getOperations();

        // Assert
        assertEquals(requestId, result.getRequestId());
        assertEquals(7, opResults.size());

        // Create cache assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(0).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 0);

        // Add and get item assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(1).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 1);
        assertEquals("value1", opResults.get(2).getValue().value());

        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(3).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 4);
        assertEquals("value2", opResults.get(4).getValue().value());

        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(5).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 6);
        assertEquals("value3", opResults.get(6).getValue().value());

        verify(sut.subscriptionService, times(3)).handleEntryAdded(
                any(AddCacheEntryResult.class),
                any(AbstractMutableDirectBuffer.class),
                any(ReusableString.class),
                any(ReusableString.class),
                anyInt());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startBulkOpsRequest(any(BulkCacheOpsRequestDetails.class));
        verify(tracingService, times(1)).endBulkOpsRequest(any(BulkCacheOpsRequestDetails.class));
    }

    @Test
    @DisplayName("Should merge a patch into an existing entry within a bulk operation")
    @HappyPath
    void shouldHandlePatchItemInBulk() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);

        var requestId = UUID.randomUUID().toString();
        List<CacheOperationRequest> ops = List.of(
            getCacheOperation(BulkOperationType.CREATE_CACHE, CACHE_ID, "", "", 0),
            getCacheOperation(BulkOperationType.ADD_ITEM, CACHE_ID, "key", "{\"a\":1,\"b\":2}", 0),
            getCacheOperation(BulkOperationType.PATCH_ITEM, CACHE_ID, "key", "{\"b\":3,\"c\":4}", 0),
            getCacheOperation(BulkOperationType.GET_ITEM, CACHE_ID, "key", "", 0)
        );

        BulkCacheOpsRequest bulkRequest = new BulkCacheOpsRequest(requestId, ops);
        int length = cacheRequestEncoder.encodeBulkOperations(requestId, bulkRequest, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeBulkCacheOpsResult(responseBuffer, 0, result);
        var opResults = result.getOperations();

        // Assert
        assertEquals(requestId, result.getRequestId());
        assertEquals(4, opResults.size());

        // Create cache assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(0).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 0);

        // Add item assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(1).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 1);

        // Patch item assertions - the response carries only the status, not the merged value
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(2).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 2);

        // Get item assertions - the merged value proves the patch was applied
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(3).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 3);
        assertEquals("{\"a\":1,\"b\":3,\"c\":4}", opResults.get(3).getValue().value());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startBulkOpsRequest(any(BulkCacheOpsRequestDetails.class));
        verify(tracingService, times(1)).endBulkOpsRequest(any(BulkCacheOpsRequestDetails.class));
    }

}
