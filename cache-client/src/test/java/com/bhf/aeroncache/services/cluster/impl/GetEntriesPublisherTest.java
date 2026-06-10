package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.matchers.GreaterThan;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetEntriesPublisherTest {

    ClusterMessagePublisher sut;

    @Mock
    CacheRequestEncoder cacheRequestEncoder;

    @Mock
    private AeronCache cluster;

    @Mock
    private IdleStrategy idleStrategy;

    @BeforeEach
    void setup() {
        sut = new ClusterMessagePublisher(cluster, idleStrategy, cacheRequestEncoder);
    }

    @Test
    @HappyPath
    @DisplayName("Should correctly encode get all entries request and offer to cluster")
    void shouldEncodeClearCacheRequestAndOfferToCluster() {
        // Arrange
        var cacheId = "123L";
        var requestId = UUID.randomUUID().toString();

        // Act
        sut.getCacheEntries(requestId, cacheId);

        // Assert
        verify(cacheRequestEncoder, times(1)).encodeGetCacheEntries(
                eq(requestId), eq(cacheId), any(MutableDirectBuffer.class)
        );

        verify(cluster, atMostOnce()).offer(
                any(MutableDirectBuffer.class),
                eq(0),
                intThat(isGreaterThanZero()));
    }

    @Test
    @HappyPath
    @DisplayName("Should poll egress pending get all cache entries request")
    void shouldPollEgressAndIdlePendingGetCacheEntryRequest() {
        // Arrange
        var cacheId = "123L";
        var requestId = UUID.randomUUID().toString();
        when(cluster.pollEgress()).thenReturn(1);

        // Act
        sut.getCacheEntriesBlocking(requestId, cacheId);

        // Assert
        verify(cluster, times(1)).pollEgress();
    }

    private static GreaterThan<Integer> isGreaterThanZero() {
        return new GreaterThan<>(0);
    }

}