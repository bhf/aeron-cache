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
class BulkOpsMultiCacheTest {

    public static final String FIRST_CACHE_ID = "bulk-multi-cache-1";
    public static final String SECOND_CACHE_ID = "bulk-multi-cache-2";
    private final Header header = new Header(0, 0);
    private final CacheResponseDecoder cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    private final CacheRequestEncoder cacheRequestEncoder = new RegularStringCacheRequestEncoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private BulkCacheOpsResult<ReusableString,ReusableString,ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private ClientSession session;

    @BeforeEach
    void setup() {
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier,SupplierUtils.stringSupplier,SupplierUtils.stringSupplier);
        session = TestUtils.getMockedSession(responseBuffer);
        TestUtils.createCache(FIRST_CACHE_ID, session, requestBuffer, sut);
        TestUtils.createCache(SECOND_CACHE_ID, session, requestBuffer, sut);
    }

    @Test
    @DisplayName("Should handle bulk operations")
    @HappyPath
    void shouldHandleBulkOperation() {
        // Arrange

        var requestId = UUID.randomUUID().toString();
        List<CacheOperationRequest> ops = List.of(
                getCacheOperation(BulkOperationType.ADD_ITEM, FIRST_CACHE_ID, "key", "value1", 0),
                getCacheOperation(BulkOperationType.GET_ITEM, FIRST_CACHE_ID, "key", "", 0),

                getCacheOperation(BulkOperationType.ADD_ITEM, SECOND_CACHE_ID, "key", "value2", 0),
                getCacheOperation(BulkOperationType.GET_ITEM, SECOND_CACHE_ID, "key", "", 0),

                getCacheOperation(BulkOperationType.REMOVE_ITEM, FIRST_CACHE_ID, "key", "", 0),
                getCacheOperation(BulkOperationType.REMOVE_ITEM, SECOND_CACHE_ID, "key", "", 0)
        );

        BulkCacheOpsRequest bulkRequest = new BulkCacheOpsRequest(requestId, ops);
        int length = cacheRequestEncoder.encodeBulkOperations(requestId, bulkRequest, requestBuffer);

        // Act
        sut.onSessionMessage(session, System.currentTimeMillis(), requestBuffer, 0, length, header);
        cacheResponseDecoder.decodeBulkCacheOpsResult(responseBuffer, 0, result);
        var opResults = result.getOperations();

        // Assert
        assertEquals(requestId, result.getRequestId());
        assertEquals(6, opResults.size());

        // Add and get item assertions
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(0).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 0);
        assertEquals("value1", opResults.get(1).getValue().value());

        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(2).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 2);
        assertEquals("value2", opResults.get(3).getValue().value());

        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(4).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 4);

        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(5).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 5);

        verify(sut.subscriptionService, times(2)).handleEntryAdded(
                any(AddCacheEntryResult.class),
                any(AbstractMutableDirectBuffer.class),
                any(ReusableString.class),
                any(ReusableString.class),
                anyInt());

        // Calling the tracing service is part of the public API of the SUT
        verify(tracingService, times(1)).startBulkOpsRequest(any(BulkCacheOpsRequestDetails.class));
        verify(tracingService, times(1)).endBulkOpsRequest(any(BulkCacheOpsRequestDetails.class));
    }


}
