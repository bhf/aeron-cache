package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

@DisplayName("Get All Cache Entries - RBCacheRequestPublisher")
class GetAllCacheEntriesRBPublisherTest extends AbstractRBPublisherOperationTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCacheRequestPublisher(rb);
    }

    @Override
    void invokeWithNullRequestId() {
        sut.getCacheEntries(null, "123L");
    }

    @Override
    void invokeWithValidRequestId() {
        sut.getCacheEntries(UUID.randomUUID().toString(), "123L");
    }
}