package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.models.results.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class CacheResponseMapObserversTest {

    CacheResponseMapObservers sut;

    @BeforeEach
    void setup() {
        sut = new CacheResponseMapObservers();
    }

    @Test
    @DisplayName("Should call existing observer for cache entry result")
    @HappyPath
    void shouldCallExistingObserversForCacheEntryResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var key = "someKey";
        var resultConsumer = Mockito.mock(Consumer.class);
        sut.getCacheEntry(requestId, cacheId, key, resultConsumer);
        sut.getCacheEntryConsumer = resultConsumer;

        var result = Mockito.mock(GetCacheEntryResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleCacheEntryResult(result);

        // Assert
        verify(resultConsumer, times(2))
                .accept(any(GetCacheEntryResult.class));

        assertFalse(sut.getCacheEntryObservers.containsKey(requestId));
        assertEquals(resultConsumer, sut.getCacheEntryConsumer);
    }

    @Test
    @DisplayName("Should call existing observer for cache entries result")
    @HappyPath
    void shouldCallExistingObserversForCacheEntriesResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var resultConsumer = Mockito.mock(Consumer.class);
        sut.getCacheEntries(requestId, cacheId, resultConsumer);
        sut.getCacheEntriesConsumer = resultConsumer;

        var result = Mockito.mock(GetAllCacheEntriesResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleAllCacheEntries(result);

        // Assert
        verify(resultConsumer, times(2))
                .accept(any(GetAllCacheEntriesResult.class));

        assertFalse(sut.getCacheEntriesObservers.containsKey(requestId));
        assertEquals(resultConsumer, sut.getCacheEntriesConsumer);
    }

    @Test
    @DisplayName("Should call existing observer for cache created result")
    @HappyPath
    void shouldCallExistingObserversForCacheCreatedResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var resultConsumer = Mockito.mock(Consumer.class);
        sut.sendCreateCache(requestId, cacheId, resultConsumer);
        sut.createCacheConsumer = resultConsumer;

        var result = Mockito.mock(CreateCacheResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleCacheCreated(result);

        // Assert
        verify(resultConsumer, times(2))
                .accept(any(CreateCacheResult.class));

        assertFalse(sut.createCacheObservers.containsKey(requestId));
        assertEquals(resultConsumer, sut.createCacheConsumer);
    }

    @Test
    @DisplayName("Should call existing observer for cache entry created result")
    @HappyPath
    void shouldCallExistingObserversForCacheEntryCreatedResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var resultConsumer = Mockito.mock(Consumer.class);
        sut.addCacheEntry(requestId, cacheId, "someKey", "someValue", resultConsumer);
        sut.addCacheEntryConsumer = resultConsumer;

        var result = Mockito.mock(AddCacheEntryResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleCacheEntryCreated(result);

        // Assert
        verify(resultConsumer, times(2))
                .accept(any(AddCacheEntryResult.class));

        assertFalse(sut.addCacheEntryObservers.containsKey(requestId));
        assertEquals(resultConsumer, sut.addCacheEntryConsumer);
    }

    @Test
    @DisplayName("Should call existing observer for cache entry removed result")
    @HappyPath
    void shouldCallExistingObserversForCacheEntryRemovedResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var resultConsumer = Mockito.mock(Consumer.class);
        sut.removeCacheEntry(requestId, cacheId, "someKey", resultConsumer);
        sut.removeCacheEntryConsumer = resultConsumer;

        var result = Mockito.mock(RemoveCacheEntryResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleCacheEntryRemoved(result);

        // Assert
        verify(resultConsumer, times(2))
                .accept(any(RemoveCacheEntryResult.class));

        assertFalse(sut.removeCacheEntryObservers.containsKey(requestId));
        assertEquals(resultConsumer, sut.removeCacheEntryConsumer);
    }

    @Test
    @DisplayName("Should call existing observer for cache cleared result")
    @HappyPath
    void shouldCallExistingObserversForCacheClearedResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var resultConsumer = Mockito.mock(Consumer.class);
        sut.clearCache(requestId, cacheId, resultConsumer);
        sut.clearCacheConsumer = resultConsumer;

        var result = Mockito.mock(ClearCacheResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleCacheCleared(result);

        // Assert
        verify(resultConsumer, times(2))
                .accept(any(ClearCacheResult.class));

        assertFalse(sut.clearCacheObservers.containsKey(requestId));
        assertEquals(resultConsumer, sut.clearCacheConsumer);
    }

    @Test
    @DisplayName("Should call existing observer for cache deleted result")
    @HappyPath
    void shouldCallExistingObserversForCacheDeletedResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var cacheId = "123L";
        var resultConsumer = Mockito.mock(Consumer.class);
        sut.deleteCache(requestId, cacheId, resultConsumer);
        sut.deleteCacheConsumer = resultConsumer;

        var result = Mockito.mock(DeleteCacheResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleCacheDeleted(result);

        // Assert
        verify(resultConsumer, times(2))
                .accept(any(DeleteCacheResult.class));

        assertFalse(sut.deleteCacheObservers.containsKey(requestId));
        assertEquals(resultConsumer, sut.deleteCacheConsumer);
    }

    @Test
    @DisplayName("Should call existing observer for cache stats result")
    @HappyPath
    void shouldCallExistingObserversForCacheStatsResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var resultConsumer = Mockito.mock(Consumer.class);
        sut.getAllCacheStats(requestId, resultConsumer);

        var result = Mockito.mock(CacheStatsResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleAllCacheStats(result);

        // Assert
        verify(resultConsumer, times(1))
                .accept(any(CacheStatsResult.class));

        assertFalse(sut.allCacheStatsObservers.containsKey(requestId));
    }

    @Test
    @DisplayName("Should call existing observer for cache subscribe result")
    @HappyPath
    void shouldCallExistingObserversForCacheSubscribeResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var resultConsumer = Mockito.mock(Consumer.class);
        var cacheId = "123L";
        sut.sendCacheSubscribe(requestId, cacheId, resultConsumer);

        var result = Mockito.mock(CacheSubscriptionResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleCacheSubscribeResponse(result);

        // Assert
        verify(resultConsumer, times(1))
                .accept(any(CacheSubscriptionResult.class));

        assertFalse(sut.cacheSubscribeObservers.containsKey(requestId));
    }

    @Test
    @DisplayName("Should call existing observer for cache unsubscribe result")
    @HappyPath
    void shouldCallExistingObserversForCacheUnsubscribeResult() {
        // Arrange
        var requestId = UUID.randomUUID().toString();
        var resultConsumer = Mockito.mock(Consumer.class);
        var cacheId = "123L";
        sut.sendCacheUnsubscribe(requestId, cacheId, resultConsumer);

        var result = Mockito.mock(CacheUnsubscribeResult.class);
        when(result.getRequestId()).thenReturn(requestId);

        // Act
        sut.handleCacheUnsubscribeResponse(result);

        // Assert
        verify(resultConsumer, times(1))
                .accept(any(CacheUnsubscribeResult.class));

        assertFalse(sut.cacheUnsubscribeObservers.containsKey(requestId));
    }

}