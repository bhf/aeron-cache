package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.utils.RingBufferUtils;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheClientAgentTest {

    CacheClientAgent sut;

    @Mock
    AeronCache cluster;
    @Mock
    IdleStrategy idleStrategy;
    ClusterMessagePublisher publisher;
    ManyToOneRingBuffer rb;

    @BeforeEach
    void setup() {
        publisher = Mockito.mock(ClusterMessagePublisher.class);
        rb = RingBufferUtils.buildRingbuffer(4096);
        sut = new CacheClientAgent(cluster, rb, idleStrategy, publisher, "AeronCache-CacheClient-Agent");
    }

    @Test
    @DisplayName("Should poll egress and idle as part of a single duty cycle")
    void shouldPollEgressAndIdleInDutyCycle() {
        // Act
        sut.runSingleCycle();

        // Assert
        verify(cluster, atMostOnce()).pollEgress();
        verify(idleStrategy, atMostOnce()).idle();
    }

    @Test
    @DisplayName("Should send KeepAlive based on time")
    void shouldSendKeepAliveBasedOnTime() {
        // Arrange
        sut.setLastKeepAlive(0);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(cluster, atMostOnce()).sendKeepAlive();
        assertTrue(sut.getLastKeepAlive() > 0);
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish create cache request via Publisher only once")
    @MethodSource("provideCreateCacheParams")
    void shouldPublishCreateCacheRequest(String requestId, String cacheId) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.sendCreateCache(requestId, cacheId);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(sut.getPublisher(), times(1)).sendCreateCache(requestId, cacheId);
    }

    public static Stream<Arguments> provideCreateCacheParams() {
        return Stream.of(
                Arguments.of("", "testCacheId"),
                Arguments.of("requestID", "testCacheId"));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish add cache entry request via Publisher only once")
    @MethodSource("provideAddCacheEntryParams")
    void shouldPublishAddCacheEntryRequest(String requestId, String cacheId, String key, String value) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.addCacheEntry(requestId, cacheId, key, value, 123);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).addCacheEntry(requestId, cacheId, key, value, 123);
    }

    public static Stream<Arguments> provideAddCacheEntryParams() {
        return Stream.of(
                Arguments.of("", "testCacheId", "key", "value"),
                Arguments.of("requestID", "testCacheId", "key", "value"),
                Arguments.of(UUID.randomUUID().toString(), "123L", "key", "value"));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish get cache entry request via Publisher only once")
    @MethodSource("provideGetCacheEntryParams")
    void shouldPublishGetCacheEntryRequest(String requestId, String cacheId, String key) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.getCacheEntry(requestId, cacheId, key);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).getCacheEntry(requestId, cacheId, key);
    }

    public static Stream<Arguments> provideGetCacheEntryParams() {
        return Stream.of(
                Arguments.of("", "testCacheId", "key"),
                Arguments.of("requestID", "testCacheId", "key"),
                Arguments.of(UUID.randomUUID().toString(), "123L", "key"));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish clear cache request via Publisher only once")
    @MethodSource("provideClearCacheParams")
    void shouldPublishClearCacheRequest(String requestId, String cacheId) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.clearCache(requestId, cacheId);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).clearCache(requestId, cacheId);
    }

    public static Stream<Arguments> provideClearCacheParams() {
        return Stream.of(
                Arguments.of("", "testCacheId"),
                Arguments.of("requestID", "testCacheId"),
                Arguments.of(UUID.randomUUID().toString(), "123L"));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish delete cache request via Publisher only once")
    @MethodSource("provideDeleteCacheParams")
    void shouldPublishDeleteCacheRequest(String requestId, String cacheId) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.deleteCache(requestId, cacheId);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).deleteCache(requestId, cacheId);
    }

    public static Stream<Arguments> provideDeleteCacheParams() {
        return Stream.of(
                Arguments.of("", "testCacheId"),
                Arguments.of("requestID", "testCacheId"),
                Arguments.of(UUID.randomUUID().toString(), "123L"));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish get cache entries request via Publisher only once")
    @MethodSource("provideGetEntriesParams")
    void shouldPublishGetCacheEntriesRequest(String requestId, String cacheId) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.getCacheEntries(requestId, cacheId);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).getCacheEntries(requestId, cacheId);
    }

    public static Stream<Arguments> provideGetEntriesParams() {
        return Stream.of(
                Arguments.of("", "testCacheId"),
                Arguments.of("requestID", "testCacheId"),
                Arguments.of(UUID.randomUUID().toString(), "123L"));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish cache subscribe request via Publisher only once")
    @MethodSource("provideCacheSubscribeParams")
    void shouldPublishCacheSubscribeRequest(String requestId, String cacheId) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.sendCacheSubscribe(requestId, cacheId);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).sendCacheSubscribe(requestId, cacheId);
    }

    public static Stream<Arguments> provideCacheSubscribeParams() {
        return Stream.of(
                Arguments.of("", "testCacheId"),
                Arguments.of("requestID", "testCacheId"),
                Arguments.of(UUID.randomUUID().toString(), "123L"));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish cache unsubscribe request via Publisher only once")
    @MethodSource("provideCacheUnsubscribeParams")
    void shouldPublishCacheUnsubscribeRequest(String requestId, String cacheId) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.sendCacheUnsubscribe(requestId, cacheId);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).sendCacheUnsubscribe(requestId, cacheId);
    }

    public static Stream<Arguments> provideCacheUnsubscribeParams() {
        return Stream.of(
                Arguments.of("", "testCacheId"),
                Arguments.of("requestID", "testCacheId"),
                Arguments.of(UUID.randomUUID().toString(), "123L"));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish get cache stats request via Publisher only once")
    @MethodSource("provideGetCacheStatsParams")
    void shouldPublishGetCacheStatsRequest(String requestId) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.getAllCacheStats(requestId);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).getAllCacheStats(requestId);
    }

    public static Stream<Arguments> provideGetCacheStatsParams() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("requestID"),
                Arguments.of(UUID.randomUUID().toString()));
    }

    @ParameterizedTest
    @HappyPath
    @DisplayName("Should publish remove cache entry request via Publisher only once")
    @MethodSource("provideRemoveCacheEntryParams")
    void shouldPublishRemoveCacheEntryRequest(String requestId, String cacheId, String key) {
        // Arrange
        RBCacheRequestPublisher requestPublisher = new RBCacheRequestPublisher(rb);
        requestPublisher.removeCacheEntry(requestId, cacheId, key);

        // Act
        sut.runSingleCycle();

        // Assert
        verify(publisher, times(1)).removeCacheEntry(requestId, cacheId, key);
    }

    public static Stream<Arguments> provideRemoveCacheEntryParams() {
        return Stream.of(
                Arguments.of("", "testCacheId", "key"),
                Arguments.of("requestID", "testCacheId", "key"),
                Arguments.of(UUID.randomUUID().toString(), "123L", "key"));
    }

}