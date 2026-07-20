package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Get All Cache Stats - RBCacheRequestPublisher")
class GetAllCacheStatsRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCacheRequestPublisher(rb);
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