package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Unsubscribe Cache - RBCountersRequestPublisher")
class UnsubscribeCounterCacheRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCountersRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.sendCacheUnsubscribe(null, "123L");
    }

    @Override
    void invokeWithValidRequestId() {
        sut.sendCacheUnsubscribe(UUID.randomUUID().toString(), "123L");
    }
}
