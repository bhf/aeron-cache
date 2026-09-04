package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Subscribe Cache - RBCacheRequestPublisher")
class SubscribeCacheRBPublisherTest extends AbstractSubscribePublisherTest {

    @Override
    AbstractRBRequestPublisher<?> createSut(RingBuffer rb) {
        return new RBCacheRequestPublisher(rb);
    }

    @Override
    int expectedSubscribeMsgId() {
        return CacheRequestMessageTypes.SUBSCRIBE_TO_CACHE_MSG_ID;
    }
}