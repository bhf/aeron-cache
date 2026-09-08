package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Cancel Item Removal - RBCountersRequestPublisher")
class CancelCounterItemRemovalRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCountersRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.cancelItemRemoval(null, "123L", "someKey");
    }

    @Override
    void invokeWithValidRequestId() {
        sut.cancelItemRemoval(UUID.randomUUID().toString(), "123L", "someKey");
    }
}
