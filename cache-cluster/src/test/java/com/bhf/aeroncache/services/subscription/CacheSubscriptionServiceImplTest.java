package com.bhf.aeroncache.services.subscription;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import com.bhf.aeroncache.models.requests.CacheUnsubscribeRequestDetails;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CacheSubscriptionServiceImplTest {

    CacheSubscriptionService<ReusableLong> sut;

    @Mock
    IdleStrategy idleStrategy;

    @Mock
    ClientSession session;

    private final long KNOWN_CACHE = 123L;


    @BeforeEach
    void setup() {
        CacheSubscriptionResult<ReusableLong> subscriptionResult = new CacheSubscriptionResult<>(new ReusableLong());
        CacheUnsubscribeResult<ReusableLong> unsubscribeResult = new CacheUnsubscribeResult<>(new ReusableLong());
        sut = new CacheSubscriptionServiceImpl<>(idleStrategy, subscriptionResult, unsubscribeResult,
                SupplierUtils.longSupplier);
    }

    @Test
    @DisplayName("Should notify on duplicate subscription")
    void shouldNotifyOnDuplicateSubscription() {
        // Arrange
        var result = subscribeToCache();

        // Act
        result = subscribeToCache();

        // Assert
        assertEquals(OperationStatus.DUPLICATE_SUBSCRIPTION, result.getStatus());
    }

    @Test
    @DisplayName("Should notify on unknown subscription on unsubscribe")
    void shouldNotifyOnUnknownSubscriptionOnUnsubscribe() {
        // Arrange
        subscribeToCache();

        var unsubscribeRequest = new CacheUnsubscribeRequestDetails<>(new ReusableLong());
        unsubscribeRequest.setRequestId(UUID.randomUUID().toString());
        unsubscribeRequest.getCacheId().copyFrom(KNOWN_CACHE);

        // Act
        var unknownSession = Mockito.mock(ClientSession.class);
        var result = sut.unsubscribe(unsubscribeRequest, unknownSession);

        // Assert
        assertEquals(OperationStatus.UNKNOWN_SUBSCRIPTION, result.getStatus());
    }

    @Test
    @DisplayName("Should offer response to session on valid cache delete request")
    @HappyPath
    void shouldOfferToSessionOnValidCacheDeleteRequest() {
        // Arrange
        subscribeToCache();

        // Act
        DeleteCacheResult<ReusableLong> requestDetails = new DeleteCacheResult<>(new ReusableLong());
        requestDetails.getCacheId().copyFrom(KNOWN_CACHE);
        MutableDirectBuffer egressBuffer = Mockito.mock(MutableDirectBuffer.class);
        CacheDeletedEncoder deleteEncoder = Mockito.mock(CacheDeletedEncoder.class);
        MessageHeaderEncoder headerEncoder = Mockito.mock(MessageHeaderEncoder.class);
        sut.handleDeleteCache(requestDetails, egressBuffer, deleteEncoder, headerEncoder);

        // Assert
        verify(session).offer(any(MutableDirectBuffer.class), eq(0), anyInt());
    }

    @Test
    @DisplayName("Should offer response to session on valid cache clear request")
    @HappyPath
    void shouldOfferToSessionOnValidCacheClearRequest() {
        // Arrange
        subscribeToCache();

        // Act
        ClearCacheResult<ReusableLong> requestDetails = new ClearCacheResult<>(new ReusableLong());
        requestDetails.getCacheId().copyFrom(KNOWN_CACHE);
        MutableDirectBuffer egressBuffer = Mockito.mock(MutableDirectBuffer.class);
        CacheClearedEncoder clearEncoder = Mockito.mock(CacheClearedEncoder.class);
        MessageHeaderEncoder headerEncoder = Mockito.mock(MessageHeaderEncoder.class);
        sut.handleClearCache(requestDetails, egressBuffer, clearEncoder, headerEncoder);

        // Assert
        verify(session).offer(any(MutableDirectBuffer.class), eq(0), anyInt());
    }

    @Test
    @DisplayName("Should offer response to session on valid remove request")
    @HappyPath
    void shouldOfferToSessionOnValidRemoveRequest() {
        // Arrange
        subscribeToCache();

        // Act
        RemoveCacheEntryResult<ReusableLong, ReusableString> requestDetails =
                new RemoveCacheEntryResult<>(new ReusableLong(), new ReusableString());
        requestDetails.getCacheId().copyFrom(KNOWN_CACHE);
        MutableDirectBuffer egressBuffer = Mockito.mock(MutableDirectBuffer.class);
        CacheEntryRemovedEncoder entryRemovedEncoder = Mockito.mock(CacheEntryRemovedEncoder.class);
        MessageHeaderEncoder headerEncoder = Mockito.mock(MessageHeaderEncoder.class);
        sut.handleEntryRemoved(requestDetails, egressBuffer, entryRemovedEncoder, headerEncoder);

        // Assert
        verify(session).offer(any(MutableDirectBuffer.class), eq(0), anyInt());
    }

    @Test
    @DisplayName("Should offer response to session on valid add entry request")
    @HappyPath
    void shouldOfferToSessionOnValidAddEntryRequest() {
        // Arrange
        subscribeToCache();

        // Act
        AddCacheEntryResult<ReusableLong, ReusableString> requestDetails =
                new AddCacheEntryResult<>(new ReusableLong(), new ReusableString());
        requestDetails.getCacheId().copyFrom(KNOWN_CACHE);
        MutableDirectBuffer egressBuffer = Mockito.mock(MutableDirectBuffer.class);
        CacheEntryCreatedEncoder addEntryEncoder = Mockito.mock(CacheEntryCreatedEncoder.class);
        MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        var key = new ReusableString();
        key.copyFrom("key");
        var value = new ReusableString();
        value.copyFrom("value");
        sut.handleEntryAdded(requestDetails, egressBuffer, key, value, addEntryEncoder, headerEncoder);

        // Assert
        verify(session).offer(any(MutableDirectBuffer.class), eq(0), anyInt());
    }

    private CacheSubscriptionResult<ReusableLong> subscribeToCache() {
        var subscribeRequest = new CacheSubscriptionRequestDetails<>(new ReusableLong());
        subscribeRequest.setRequestId(UUID.randomUUID().toString());
        subscribeRequest.getCacheId().copyFrom(KNOWN_CACHE);
        return sut.subscribe(subscribeRequest, session);
    }

}