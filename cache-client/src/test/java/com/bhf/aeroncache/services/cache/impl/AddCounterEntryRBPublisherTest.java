package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.annotations.HappyPath;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.matchers.GreaterThan;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Add Counter Entry - RBCountersRequestPublisher")
class AddCounterEntryRBPublisherTest {

    RBCountersRequestPublisher sut;

    @Mock
    RingBuffer rb;

    @BeforeEach
    void setup() {
        sut = new RBCountersRequestPublisher(rb);
    }

    @Test
    @DisplayName("Should throw NPE on null requestId without interacting with RingBuffer when adding counter entry")
    void shouldThrowExceptionOnNullRequestId() {
        // Arrange
        var cacheId = "123L";
        var key = "someKey";
        var value = 42L;

        // Act + Assert
        Assertions.assertThrows(NullPointerException.class,
                () -> sut.addCacheEntry(null, cacheId, key, value, 0));

        verifyNoInteractions(rb);
    }

    @Test
    @DisplayName("Should abort claim on RingBuffer on RuntimeException when adding counter entry")
    void shouldAbortOnRingBufferOnException() {
        // Arrange
        var cacheId = "123L";
        var key = "someKey";
        var value = 42L;
        var requestId = UUID.randomUUID().toString();
        when(rb.buffer()).thenThrow(RuntimeException.class);

        // Act
        sut.addCacheEntry(requestId, cacheId, key, value, 0);

        // Assert
        verify(rb, atMostOnce()).abort(intThat(isGreaterThanZero()));
    }

    @Test
    @HappyPath
    @DisplayName("Should commit claim on RingBuffer when adding counter entry")
    void shouldCommitClaimOnRBWhenAddingCounterEntry() {
        // Arrange
        var cacheId = "123L";
        var key = "someKey";
        var value = 42L;
        var requestId = UUID.randomUUID().toString();
        var mockBuffer = Mockito.mock(AtomicBuffer.class);
        when(rb.buffer()).thenReturn(mockBuffer);

        // Act
        sut.addCacheEntry(requestId, cacheId, key, value, 0);

        // Assert
        verify(rb, atMostOnce()).commit(intThat(isGreaterThanZero()));
    }

    private static ArgumentMatcher<Integer> isGreaterThanZero() {
        return new GreaterThan<>(0);
    }
}
