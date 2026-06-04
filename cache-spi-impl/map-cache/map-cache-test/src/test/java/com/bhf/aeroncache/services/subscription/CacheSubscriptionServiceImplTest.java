package com.bhf.aeroncache.services.subscription;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import com.bhf.aeroncache.models.requests.CacheUnsubscribeRequestDetails;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CacheSubscriptionServiceImplTest {

    CacheSubscriptionService<ReusableString, ReusableString, ReusableString> sut;

    @Mock
    IdleStrategy idleStrategy;

    @Mock
    ClientSession session;

    private final String KNOWN_CACHE = "123L";


    @BeforeEach
    void setup() {
        CacheSubscriptionResult<ReusableString,ReusableString,ReusableString> subscriptionResult = new CacheSubscriptionResult<>(new ReusableString());
        CacheUnsubscribeResult<ReusableString> unsubscribeResult = new CacheUnsubscribeResult<>(new ReusableString());
        sut = new CacheSubscriptionServiceImpl<>(idleStrategy, subscriptionResult, unsubscribeResult,
                SupplierUtils.stringSupplier);
    }

    @Test
    @DisplayName("Should notify on duplicate subscription")
    void shouldNotifyOnDuplicateSubscription() {
        // Arrange
        var result = subscribeToCache();

        // Act
        result = subscribeToCache();

        // Assert
        assertEquals(CacheOperationStatus.DUPLICATE_SUBSCRIPTION, result.getStatus());
        assertEquals(KNOWN_CACHE, result.getCacheId().value());
    }

    @Test
    @DisplayName("Should notify on unknown subscription on unsubscribe")
    void shouldNotifyOnUnknownSubscriptionOnUnsubscribe() {
        // Arrange
        subscribeToCache();

        var unsubscribeRequest = new CacheUnsubscribeRequestDetails<>(new ReusableString());
        unsubscribeRequest.setRequestId(UUID.randomUUID().toString());
        unsubscribeRequest.getCacheId().copyFrom(KNOWN_CACHE);

        // Act
        var unknownSession = Mockito.mock(ClientSession.class);
        var result = sut.unsubscribe(unsubscribeRequest, unknownSession);

        // Assert
        Assertions.assertEquals(CacheOperationStatus.UNKNOWN_SUBSCRIPTION, result.getStatus());
        Assertions.assertEquals(KNOWN_CACHE, result.getCacheId().value());
    }

    @Test
    @DisplayName("Should offer response to session on valid cache delete request")
    @HappyPath
    void shouldOfferToSessionOnValidCacheDeleteRequest() {
        // Arrange
        subscribeToCache();
        Mockito.when(session.id()).thenReturn(321L);

        // Act
        DeleteCacheResult<ReusableString> requestDetails = new DeleteCacheResult<>(new ReusableString());
        requestDetails.getCacheId().copyFrom(KNOWN_CACHE);
        MutableDirectBuffer egressBuffer = Mockito.mock(MutableDirectBuffer.class);
        sut.handleDeleteCache(requestDetails, egressBuffer, 1, 0);

        // Assert
        Mockito.verify(session).offer(ArgumentMatchers.any(MutableDirectBuffer.class), ArgumentMatchers.eq(0), ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("Should offer response to session on valid cache clear request")
    @HappyPath
    void shouldOfferToSessionOnValidCacheClearRequest() {
        // Arrange
        subscribeToCache();
        Mockito.when(session.id()).thenReturn(321L);

        // Act
        ClearCacheResult<ReusableString> requestDetails = new ClearCacheResult<>(new ReusableString());
        requestDetails.getCacheId().copyFrom(KNOWN_CACHE);
        MutableDirectBuffer egressBuffer = Mockito.mock(MutableDirectBuffer.class);
        sut.handleClearCache(requestDetails, egressBuffer, 1, 0);

        // Assert
        Mockito.verify(session).offer(ArgumentMatchers.any(MutableDirectBuffer.class), ArgumentMatchers.eq(0), ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("Should offer response to session on valid remove request")
    @HappyPath
    void shouldOfferToSessionOnValidRemoveRequest() {
        // Arrange
        subscribeToCache();
        Mockito.when(session.id()).thenReturn(321L);

        // Act
        RemoveCacheEntryResult<ReusableString, ReusableString> requestDetails =
                new RemoveCacheEntryResult<>(new ReusableString(), new ReusableString());
        requestDetails.getCacheId().copyFrom(KNOWN_CACHE);
        MutableDirectBuffer egressBuffer = Mockito.mock(MutableDirectBuffer.class);
        sut.handleEntryRemoved(requestDetails, egressBuffer, 1, 0);

        // Assert
        Mockito.verify(session).offer(ArgumentMatchers.any(MutableDirectBuffer.class), ArgumentMatchers.eq(0), ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("Should offer response to session on valid add entry request")
    @HappyPath
    void shouldOfferToSessionOnValidAddEntryRequest() {
        // Arrange
        subscribeToCache();

        // Act
        AddCacheEntryResult<ReusableString, ReusableString> requestDetails =
                new AddCacheEntryResult<>(new ReusableString(), new ReusableString());
        requestDetails.getCacheId().copyFrom(KNOWN_CACHE);
        MutableDirectBuffer egressBuffer = Mockito.mock(MutableDirectBuffer.class);
        var key = new ReusableString();
        key.copyFrom("key");
        var value = new ReusableString();
        value.copyFrom("value");
        sut.handleEntryAdded(requestDetails, egressBuffer, key, value, 1);

        // Assert
        Mockito.verify(session).offer(ArgumentMatchers.any(MutableDirectBuffer.class), ArgumentMatchers.eq(0), ArgumentMatchers.anyInt());
    }

    private CacheSubscriptionResult<ReusableString,ReusableString,ReusableString> subscribeToCache() {
        var subscribeRequest = new CacheSubscriptionRequestDetails<>();
        subscribeRequest.setRequestId(UUID.randomUUID().toString());
        var cacheID = new ReusableString();
        cacheID.copyFrom(KNOWN_CACHE);
        return sut.subscribe(session, cacheID, subscribeRequest.getRequestId());
    }

}