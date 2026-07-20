package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Get Cache Entry - RBCacheRequestPublisher")
class GetCacheEntryRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCacheRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.getCacheEntry(null, "123L", "someKey");
    }

    @Override
    void invokeWithValidRequestId() {
        sut.getCacheEntry(UUID.randomUUID().toString(), "123L", "someKey");
    }
}