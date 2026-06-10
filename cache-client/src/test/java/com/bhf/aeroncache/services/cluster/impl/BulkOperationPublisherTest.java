package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.matchers.GreaterThan;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkOperationPublisherTest {

    ClusterMessagePublisher sut;

    @Mock
    CacheRequestEncoder cacheRequestEncoder;

    @Mock
    private AeronCache cluster;

    @Mock
    private IdleStrategy idleStrategy;

    @BeforeEach
    void setup() {
        sut = new ClusterMessagePublisher(cluster, idleStrategy, cacheRequestEncoder);
    }

    @Test
    @HappyPath
    @DisplayName("Should correctly encode bulk request and offer to cluster")
    void shouldEncodeBulkRequestAndOfferToCluster() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        List<CacheOperationRequest> ops = List.of(getCacheOperation(0), getCacheOperation(1));
        BulkCacheOpsRequest bulkOps = new BulkCacheOpsRequest(requestId, ops);

        // Act
        sut.sendBulkOperationsRequest(requestId, bulkOps);

        // Assert
        verify(cacheRequestEncoder, times(1)).encodeBulkOperations(
                eq(requestId), eq(bulkOps), any(MutableDirectBuffer.class)
        );

        verify(cluster, atMostOnce()).offer(
                any(MutableDirectBuffer.class),
                eq(0),
                intThat(isGreaterThanZero()));
    }

    @Test
    @HappyPath
    @DisplayName("Should poll egress pending blocking add cache entry request")
    void shouldPollEgressAndIdlePendingBulkEntryRequest() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        List<CacheOperationRequest> ops = List.of(getCacheOperation(0), getCacheOperation(1));
        BulkCacheOpsRequest bulkOps = new BulkCacheOpsRequest(requestId, ops);
        when(cluster.pollEgress()).thenReturn(1);

        // Act
        sut.sendBulkOperationsBlocking(requestId, bulkOps);

        // Assert
        verify(cluster, times(1)).pollEgress();
    }

    private CacheOperationRequest getCacheOperation(int ordinal) {
        String opRequestId = UUID.randomUUID().toString();
        String cacheId = "cacheId";
        String key = "key-"+ordinal;
        String value = "value";
        return new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0, opRequestId, cacheId, key, value);
    }

    private static GreaterThan<Integer> isGreaterThanZero() {
        return new GreaterThan<>(0);
    }
}