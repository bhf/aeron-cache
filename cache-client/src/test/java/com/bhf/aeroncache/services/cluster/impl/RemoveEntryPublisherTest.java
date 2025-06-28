package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import com.bhf.aeroncache.messages.RemoveCacheEntryEncoder;
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
class RemoveEntryPublisherTest {

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
    @DisplayName("Should correctly encode remove entry request and offer to cluster")
    void shouldEncodeRemvoveEntryRequestAndOfferToCluster() {
        // Arrange
        var cacheId = 123L;
        var requestId = UUID.randomUUID().toString();
        var key = "someKey";

        try (MockedStatic<CacheRequestEncoder> encoder = Mockito.mockStatic(CacheRequestEncoder.class)) {
            // Act
            sut.removeCacheEntry(requestId, cacheId, key);

            // Assert
            encoder.verify(() ->
                            CacheRequestEncoder.encodeRemoveCacheEntry(
                                    any(RemoveCacheEntryEncoder.class),
                                    any(MessageHeaderEncoder.class),
                                    any(MutableDirectBuffer.class),
                                    eq(requestId),
                                    eq(cacheId),
                                    eq(key)),
                    times(1));

            verify(cluster, atMostOnce()).offer(
                    any(MutableDirectBuffer.class),
                    eq(0),
                    intThat(isGreaterThanZero()));
        }
    }

    @Test
    @HappyPath
    @DisplayName("Should poll egress pending blocking remove entry request")
    void shouldPollEgressAndIdlePendingRemoveEntryRequest() {
        // Arrange
        var cacheId = 123L;
        var requestId = UUID.randomUUID().toString();
        var key = "someKey";
        when(cluster.pollEgress()).thenReturn(1);

        // Act
        sut.removeCacheEntryBlocking(requestId, cacheId, key);

        // Assert
        verify(cluster, times(1)).pollEgress();
    }

    private static GreaterThan<Integer> isGreaterThanZero() {
        return new GreaterThan<>(0);
    }

}