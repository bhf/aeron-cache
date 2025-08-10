package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.messages.CacheUnsubscribeRequestEncoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.internal.matchers.GreaterThan;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnsubscribeCachePublisherTest {

    ClusterMessagePublisher sut;

    @Mock
    private AeronCache cluster;

    @Mock
    private IdleStrategy idleStrategy;

    @BeforeEach
    void setup() {
        sut = new ClusterMessagePublisher(cluster, idleStrategy);
    }

    @Test
    @HappyPath
    @DisplayName("Should correctly encode cache unsubscribe request and offer to cluster")
    void shouldEncodeCacheUnsubscribeRequestAndOfferToCluster() {
        // Arrange
        var cacheId = "123L";
        var requestId = UUID.randomUUID().toString();

        try (MockedStatic<CacheRequestEncoder> encoder = Mockito.mockStatic(CacheRequestEncoder.class)) {
            // Act
            sut.sendCacheUnsubscribe(requestId, cacheId);

            // Assert
            encoder.verify(() ->
                            CacheRequestEncoder.encodeCacheUnsubscribe(
                                    any(CacheUnsubscribeRequestEncoder.class),
                                    any(MessageHeaderEncoder.class),
                                    any(MutableDirectBuffer.class),
                                    eq(requestId),
                                    eq(cacheId)),
                    times(1));

            verify(cluster, atMostOnce()).offer(
                    any(MutableDirectBuffer.class),
                    eq(0),
                    intThat(isGreaterThanZero()));
        }
    }

    @Test
    @HappyPath
    @DisplayName("Should poll egress pending blocking cache unsubscribe request")
    void shouldPollEgressAndIdlePendingCacheUnsubscribeRequest() {
        // Arrange
        var cacheId = "123L";
        var requestId = UUID.randomUUID().toString();
        when(cluster.pollEgress()).thenReturn(1);

        // Act
        sut.sendCacheUnsubscribeBlocking(requestId, cacheId);

        // Assert
        verify(cluster, times(1)).pollEgress();
    }

    private static GreaterThan<Integer> isGreaterThanZero() {
        return new GreaterThan<>(0);
    }

}