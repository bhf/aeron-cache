package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkPublishRBPublisherTest {

    RBCacheRequestPublisher sut;

    @Mock
    RingBuffer rb;

    @BeforeEach
    void setup() {
        sut = new RBCacheRequestPublisher(rb);
    }

    @Test
    @HappyPath
    @DisplayName("Should try to write to RingBuffer at least once")
    void shouldWriteToRingBuffer() {
        // Arrange
        when(rb.write(anyInt(), any(), anyInt(), anyInt())).thenReturn(true);
        var requestId = UUID.randomUUID().toString();
        BulkCacheOpsRequest request = getBulkCacheOpsRequest(requestId);

        // Act
        sut.sendBulkOperationsRequest(requestId, request);

        // Assert
        verify(rb, atLeastOnce())
                .write(eq(CacheRequestMessageTypes.BULK_OPS_MSG_ID), any(), eq(0), anyInt());
    }

    private BulkCacheOpsRequest getBulkCacheOpsRequest(String requestId) {
        List<CacheOperationRequest> ops = List.of(
                getCacheOperation(0),
                getCacheOperation(1),
                getCacheOperation(2)
        );
        return new BulkCacheOpsRequest(requestId, ops);
    }

    private CacheOperationRequest getCacheOperation(int ordinal) {
        String opRequestId = UUID.randomUUID().toString();
        String cacheId = "cacheId";
        String key = "key-"+ordinal;
        String value = "value";
        return new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0, opRequestId, cacheId, key, value);
    }

}