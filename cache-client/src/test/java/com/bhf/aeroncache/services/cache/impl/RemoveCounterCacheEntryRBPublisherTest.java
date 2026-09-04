package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Remove Cache Entry - RBCountersRequestPublisher")
class RemoveCounterCacheEntryRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCountersRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.removeCacheEntry(null, "123L", "someKey");
    }

    @Override
    void invokeWithValidRequestId() {
        sut.removeCacheEntry(UUID.randomUUID().toString(), "123L", "someKey");
    }
}
