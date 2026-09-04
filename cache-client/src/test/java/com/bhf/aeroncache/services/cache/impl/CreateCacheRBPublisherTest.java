package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Create Cache - RBCacheRequestPublisher")
class CreateCacheRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCacheRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.sendCreateCache(null, "123L");
    }

    @Override
    void invokeWithValidRequestId() {
        sut.sendCreateCache(UUID.randomUUID().toString(), "123L");
    }
}