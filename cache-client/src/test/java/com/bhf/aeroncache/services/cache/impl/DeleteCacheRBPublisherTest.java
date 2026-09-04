package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Delete Cache - RBCacheRequestPublisher")
class DeleteCacheRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCacheRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.deleteCache(null, "123L");
    }

    @Override
    void invokeWithValidRequestId() {
        sut.deleteCache(UUID.randomUUID().toString(), "123L");
    }
}