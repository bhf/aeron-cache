package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.annotations.HappyPath;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.matchers.GreaterThan;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCacheRBPublisherTest {

    RBCacheRequestPublisher sut;

    @Mock
    RingBuffer rb;

    @BeforeEach
    void setup() {
        sut = new RBCacheRequestPublisher(rb);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw NPE on null requestId without interacting with RingBuffer when deleting cache")
    void shouldThrowExceptionOnNullRequestId(String requestId) {
        // Arrange
        var cacheId = 123L;

        // Act + Assert
        Assertions.assertThrows(NullPointerException.class,
                () -> sut.deleteCache(requestId, cacheId));

        verifyNoInteractions(rb);
    }

    @Test
    @DisplayName("Should abort claim on RingBuffer on RuntimeException when deleting cache")
    void shouldAbortOnRingBufferOnException() {
        // Arrange
        var cacheId = 123L;
        var requestId = UUID.randomUUID().toString();
        when(rb.buffer()).thenThrow(RuntimeException.class);

        // Act
        sut.deleteCache(requestId, cacheId);

        // Assert
        verify(rb, atMostOnce()).abort(intThat(isGreaterThanZero()));
    }

    @Test
    @HappyPath
    @DisplayName("Should commit claim on RingBuffer when deleting cache")
    void shouldCommitClaimOnRBWhenDeletingCache() {
        // Arrange
        var cacheId = 123L;
        var requestId = UUID.randomUUID().toString();
        var mockBuffer = Mockito.mock(AtomicBuffer.class);
        when(rb.buffer()).thenReturn(mockBuffer);

        // Act
        sut.deleteCache(requestId, cacheId);

        // Assert
        verify(rb, atMostOnce()).commit(intThat(isGreaterThanZero()));
    }

    private static ArgumentMatcher<Integer> isGreaterThanZero() {
        return new GreaterThan<>(0);
    }

}