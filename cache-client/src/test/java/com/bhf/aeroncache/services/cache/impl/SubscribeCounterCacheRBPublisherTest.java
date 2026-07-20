package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Subscribe Cache - RBCountersRequestPublisher")
class SubscribeCounterCacheRBPublisherTest extends AbstractSubscribePublisherTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCountersRequestPublisher(rb);
    }

    @Override
    int expectedSubscribeMsgId() {
        return CacheRequestMessageTypes.SUBSCRIBE_TO_COUNTER_CACHE_MSG_ID;
    }
}
