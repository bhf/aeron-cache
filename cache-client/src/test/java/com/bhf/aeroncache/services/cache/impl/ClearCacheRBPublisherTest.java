package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Clear Cache - RBCacheRequestPublisher")
class ClearCacheRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCacheRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.clearCache(null, "123L");
    }

    @Override
    void invokeWithValidRequestId() {
        sut.clearCache(UUID.randomUUID().toString(), "123L");
    }
}