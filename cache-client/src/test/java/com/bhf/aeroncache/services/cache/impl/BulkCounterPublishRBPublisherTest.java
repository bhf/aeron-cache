package com.bhf.aeroncache.services.cache.impl;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk Operations - RBCountersRequestPublisher")
class BulkCounterPublishRBPublisherTest {

    RBCountersRequestPublisher sut;

    @Mock
    RingBuffer rb;

    @BeforeEach
    void setup() {
        sut = new RBCountersRequestPublisher(rb);
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for bulk operations on counters")
    void shouldThrowUnsupportedOperationException() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        List<CacheOperationRequest> ops = List.of(
                new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0, 0,
                        UUID.randomUUID().toString(), "cacheId", "key", "value")
        );
        var request = new BulkCacheOpsRequest(requestId, ops);

        // Act + Assert
        assertThrows(UnsupportedOperationException.class,
                () -> sut.sendBulkOperationsRequest(requestId, request));
    }
}
