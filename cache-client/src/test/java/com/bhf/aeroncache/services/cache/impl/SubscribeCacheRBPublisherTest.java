package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.annotations.HappyPath;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscribeCacheRBPublisherTest {

    RBCacheRequestPublisher sut;

    @Mock
    RingBuffer rb;

    @BeforeEach
    void setup() {
        sut = new RBCacheRequestPublisher(rb);
    }

    @Test
    @DisplayName("Should return on null requestId")
    void shouldHandleNullRequestId() {
        // Arrange
        var cacheId = "123L";

        // Act
        sut.sendCacheSubscribe(null, List.of(cacheId), false);

        // Assert
        verify(rb, never()).write(anyInt(), any(), anyInt(), anyInt());
    }

    @Test
    @HappyPath
    @DisplayName("Should write to RingBuffer when subscribing to cache")
    void shouldWriteToRBWhenSubscribingCache() {
        // Arrange
        var cacheId = "123L";
        var requestId = UUID.randomUUID().toString();
        when(rb.write(anyInt(), any(), anyInt(), anyInt())).thenReturn(true);

        // Act
        sut.sendCacheSubscribe(requestId, List.of(cacheId), false);

        // Assert
        verify(rb, atLeastOnce()).write(eq(com.bhf.aeroncache.models.CacheRequestMessageTypes.SUBSCRIBE_TO_CACHE_MSG_ID), any(), eq(0), anyInt());
    }

}