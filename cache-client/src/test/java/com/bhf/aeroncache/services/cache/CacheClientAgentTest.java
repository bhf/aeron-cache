package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.utils.RingBufferUtils;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;

class CacheClientAgentTest {
    private static final long MAX_SBE_LONG = Long.MAX_VALUE;
    private static final long MIN_SBE_LONG = -Long.MAX_VALUE;
    
    CacheClientAgent sut;
    AeronCache cluster;
    IdleStrategy idleStrategy;
    ClusterMessagePublisher publisher;
    ManyToOneRingBuffer rb;

    @BeforeEach
    void setup() {
        cluster = Mockito.mock(AeronCache.class);
        idleStrategy = Mockito.mock(IdleStrategy.class);
        publisher = Mockito.mock(ClusterMessagePublisher.class);
        rb = RingBufferUtils.buildRingbuffer(4096);
        sut = new CacheClientAgent(cluster, rb, idleStrategy, publisher);
    }

    @ParameterizedTest
    @DisplayName("Should publish create cache request via Publisher")
    @MethodSource("provideCreateCacheParams")
    void shouldPublishCreateCacheRequest(String requestId, long cacheId) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.sendCreateCache(requestId, cacheId);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher).sendCreateCache(requestId, cacheId);
    }

    public static Stream<Arguments> provideCreateCacheParams() {
        return Stream.of(
                Arguments.of("", 1L),
                Arguments.of("requestID", -1L),
                Arguments.of("requestID", MAX_SBE_LONG),
                Arguments.of("requestID", MIN_SBE_LONG),
                Arguments.of(UUID.randomUUID().toString(), 123L));
    }

}