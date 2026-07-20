package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Get All Cache Stats - RBCountersRequestPublisher")
class GetAllCounterCacheStatsRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCountersRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.getAllCacheStats(null);
    }

    @Override
    void invokeWithValidRequestId() {
        sut.getAllCacheStats(UUID.randomUUID().toString());
    }
}
