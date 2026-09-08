package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.bhf.aeroncache.models.results.BulkCacheOpsResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.services.TestUtils;
import com.bhf.aeroncache.services.subscription.CacheSubscriptionService;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.Header;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cancelling a scheduled item removal via bulk operations.
 */
@HappyPath
class BulkOpsCancelItemTest {

    public static final String CACHE_ID = "bulk";
    private final Header header = new Header(0, 0);
    private final CacheResponseDecoder cacheResponseDecoder = new ReusableStringCacheResponseDecoder();
    private final CacheRequestEncoder cacheRequestEncoder = new RegularStringCacheRequestEncoder();
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> result;
    private SBEDecodingCacheClusterService sut;
    private CacheTracingService tracingService;
    private final Cluster cluster = Mockito.mock(Cluster.class);

    @BeforeEach
    void setup() {
        when(cluster.scheduleTimer(anyLong(), anyLong())).thenReturn(true);
        when(cluster.cancelTimer(anyLong())).thenReturn(true);
        tracingService = Mockito.mock(CacheTracingService.class);
        sut = new SBEDecodingCacheClusterService("node0", tracingService, TestUtils.getCacheManagerFactory());
        sut.onStart(cluster, null);
        sut.subscriptionService = Mockito.mock(CacheSubscriptionService.class);
        sut.countersSubscriptionService = Mockito.mock(CacheSubscriptionService.class);
        responseBuffer = new ExpandableArrayBuffer();
        requestBuffer = new ExpandableArrayBuffer();
        result = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
    }

    @Test
    @DisplayName("Should cancel a scheduled item removal via bulk operations")
    @HappyPath
    void shouldCancelItemRemovalInBulk() {
        // Arrange
        ClientSession session = TestUtils.getMockedSession(responseBuffer);

        var requestId = UUID.randomUUID().toString();
        List<CacheOperationRequest> ops = List.of(
                getCacheOperation(BulkOperationType.CREATE_CACHE, CACHE_ID, "", "", 0),
                getCacheOperation(BulkOperationType.ADD_ITEM, CACHE_ID, "key", "value", 10000),
                getCacheOperation(BulkOperationType.CANCEL_ITEM, CACHE_ID, "key", "", 0),
                getCacheOperation(BulkOperationType.CANCEL_ITEM, CACHE_ID, "unknownKey", "", 0)
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

        // Create cache
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(0).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 0);

        // Add item (schedules a removal timer)
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(1).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 1);

        // Cancel scheduled removal
        assertEquals(CacheOperationStatus.SUCCESS, opResults.get(2).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 2);
        assertEquals("key", opResults.get(2).getKey().value());

        // Cancel with no scheduled removal
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, opResults.get(3).getOperationStatus());
        assertRequestIdCacheIdMatch(ops, opResults, 3);

        // A single scheduled timer was cancelled
        verify(cluster, times(1)).cancelTimer(anyLong());
    }
}
