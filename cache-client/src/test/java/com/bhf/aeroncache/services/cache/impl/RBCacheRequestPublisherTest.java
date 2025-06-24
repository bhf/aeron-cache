package com.bhf.aeroncache.services.cache.impl;

import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RBCacheRequestPublisherTest {

    RBCacheRequestPublisher sut;

    @Mock
    RingBuffer rb;

    @BeforeEach
    void setup(){
        sut = new RBCacheRequestPublisher(rb);
    }

    @ParameterizedTest
    @DisplayName("Should throw NullPointerException on null requestId and not interact with RingBuffer")
    @NullSource
    void shouldThrowExceptionOnNullRequestId(String requestId) {
        // Arrange
        var cacheId = 123L;

        // Act + Assert
        Assertions.assertThrows(NullPointerException.class,
                () -> sut.sendCreateCache(requestId, cacheId));

        verifyNoInteractions(rb);
    }

    @Test
    @DisplayName("Should abort claim on RingBuffer on RuntimeException")
    void shouldAbortOnRingBufferOnException() {
        // Arrange
        var cacheId = 123L;
        var requestId = UUID.randomUUID().toString();
        when(rb.buffer()).thenThrow(RuntimeException.class);

        // Act
        sut.sendCreateCache(requestId, cacheId);

        // Assert
        verify(rb).abort(anyInt());
    }

}