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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
abstract class AbstractRBPublisherOperationTest {

    AbstractRBRequestPublisher<?> sut;

    @Mock
    RingBuffer rb;

    abstract AbstractRBRequestPublisher<?> createSut(RingBuffer rb);

    abstract void invokeWithNullRequestId();

    abstract void invokeWithValidRequestId();

    @BeforeEach
    void setup() {
        sut = createSut(rb);
    }

    @Test
    @DisplayName("Should throw NPE on null requestId without interacting with RingBuffer")
    void shouldThrowExceptionOnNullRequestId() {
        Assertions.assertThrows(NullPointerException.class, this::invokeWithNullRequestId);
        verifyNoInteractions(rb);
    }

    @Test
    @DisplayName("Should abort claim on RingBuffer on RuntimeException")
    void shouldAbortOnRingBufferOnException() {
        when(rb.buffer()).thenThrow(RuntimeException.class);
        invokeWithValidRequestId();
        verify(rb, atMostOnce()).abort(intThat(isGreaterThanZero()));
    }

    @Test
    @HappyPath
    @DisplayName("Should commit claim on RingBuffer")
    void shouldCommitClaimOnRB() {
        var mockBuffer = Mockito.mock(AtomicBuffer.class);
        when(rb.buffer()).thenReturn(mockBuffer);
        invokeWithValidRequestId();
        verify(rb, atMostOnce()).commit(intThat(isGreaterThanZero()));
    }

    static ArgumentMatcher<Integer> isGreaterThanZero() {
        return new GreaterThan<>(0);
    }
}
