package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.gateway.metrics.GatewayMetrics;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the near-cache "warm ride" for non-hydrating subscriptions on the Aeron gateway's own subscription
 * publisher: a non-hydrating FULL subscriber on an already-warm cache must ride the near cache session's
 * existing shared cluster subscription instead of opening a redundant per-subscriber one, while cold caches,
 * PATCH mode and the internal near-cache session still subscribe to the cluster. Mirrors
 * {@code CacheSubscriptionRequestPublisherNearRideTest} for the shared (SSE/WS) publisher.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GatewaySubscriptionPublisher - non-hydrating near cache ride")
class GatewaySubscriptionPublisherNearRideTest {

    private static final String NEAR_SESSION = "gateway-near-cache";
    private static final String CLIENT_SESSION = "client-1";
    private static final String CACHE = "cache-1";
    private static final String KEY = "key-1";
    private static final String REQUEST_ID = "req-1";

    @Mock
    CacheRequestPublisher rbPublisher;

    @Mock
    NearCacheManager<Object> nearCacheManager;

    @Mock
    AeronCache cluster;

    @Mock
    GatewayMetrics metrics;

    private static final String DOMAIN = "cache";

    private final Consumer<Void> failureHandler = v -> { };
    private final Consumer<GatewayStreamUpdate> consumer = e -> { };

    @SuppressWarnings({"unchecked", "rawtypes"})
    private GatewaySubscriptionPublisher newPublisherWithNearCache() {
        var publisher = new GatewaySubscriptionPublisher(rbPublisher, nearCacheManager, NEAR_SESSION);
        publisher.setMetrics(metrics, DOMAIN);
        return publisher;
    }

    private void verifyNoClusterSubscribe() {
        verify(rbPublisher, never()).sendCacheSubscribe(anyString(), anyList(), anyList(), anyList(), anyBoolean());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Non-hydrating whole-cache FULL subscribe on a warm cache rides the shared subscription")
    void nonHydratingWholeCacheFullOnWarmCacheRidesSharedSubscription() {
        // Arrange
        var sut = newPublisherWithNearCache();
        when(nearCacheManager.contains(CACHE)).thenReturn(true);
        var acked = new AtomicBoolean(false);

        // Act
        sut.subscribeToCache(cluster, failureHandler, () -> acked.set(true), List.of(CACHE), null,
                SubscriptionMode.FULL, CLIENT_SESSION, REQUEST_ID, false, consumer);

        // Assert
        verifyNoClusterSubscribe();
        verify(metrics).onNearSubscriptionServed(DOMAIN, false);
        assertTrue(acked.get(), "ride-the-warm-cache path should ack immediately without a cluster round-trip");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Non-hydrating keyed FULL subscribe on a warm cache rides the shared subscription")
    void nonHydratingKeyedFullOnWarmCacheRidesSharedSubscription() {
        // Arrange
        var sut = newPublisherWithNearCache();
        when(nearCacheManager.contains(CACHE)).thenReturn(true);

        // Act
        sut.subscribeToCache(cluster, failureHandler, () -> { }, List.of(CACHE), List.of(KEY),
                SubscriptionMode.FULL, CLIENT_SESSION, REQUEST_ID, false, consumer);

        // Assert
        verifyNoClusterSubscribe();
        verify(metrics).onNearSubscriptionServed(DOMAIN, true);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Non-hydrating FULL subscribe on a cold cache subscribes to the cluster (no snapshot)")
    void nonHydratingFullOnColdCacheSubscribesToCluster() {
        // Arrange
        var sut = newPublisherWithNearCache();
        when(nearCacheManager.contains(CACHE)).thenReturn(false);

        // Act
        sut.subscribeToCache(cluster, failureHandler, () -> { }, List.of(CACHE), null,
                SubscriptionMode.FULL, CLIENT_SESSION, REQUEST_ID, false, consumer);

        // Assert
        verify(rbPublisher).sendCacheSubscribe(anyString(), anyList(), anyList(), anyList(), eq(false));
        verify(metrics, never()).onNearSubscriptionServed(anyString(), anyBoolean());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Non-hydrating PATCH subscribe on a warm cache still subscribes to the cluster")
    void nonHydratingPatchOnWarmCacheSubscribesToCluster() {
        // Arrange
        var sut = newPublisherWithNearCache();

        // Act
        sut.subscribeToCache(cluster, failureHandler, () -> { }, List.of(CACHE), null,
                SubscriptionMode.PATCH, CLIENT_SESSION, REQUEST_ID, false, consumer);

        // Assert - PATCH has no local baseline to apply deltas onto, so it must take the cluster path
        verify(rbPublisher).sendCacheSubscribe(anyString(), anyList(), anyList(), anyList(), eq(false));
        verify(metrics, never()).onNearSubscriptionServed(anyString(), anyBoolean());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("The internal near-cache session's own subscribe is never short-circuited")
    void nearCacheSessionSubscribeIsNotShortCircuited() {
        // Arrange - the warming session must establish the real shared cluster subscription
        var sut = newPublisherWithNearCache();

        // Act
        sut.subscribeToCache(cluster, failureHandler, () -> { }, List.of(CACHE), null,
                SubscriptionMode.FULL, NEAR_SESSION, REQUEST_ID, false, consumer);

        // Assert
        verify(rbPublisher).sendCacheSubscribe(anyString(), anyList(), anyList(), anyList(), eq(false));
        verify(metrics, never()).onNearSubscriptionServed(anyString(), anyBoolean());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    @DisplayName("With the near cache disabled, a non-hydrating subscribe always subscribes to the cluster")
    void nearCacheDisabledAlwaysSubscribesToCluster() {
        // Arrange
        var sut = new GatewaySubscriptionPublisher(rbPublisher);
        var acked = new AtomicBoolean(false);

        // Act
        sut.subscribeToCache(cluster, failureHandler, () -> acked.set(true), List.of(CACHE), null,
                SubscriptionMode.FULL, CLIENT_SESSION, REQUEST_ID, false, consumer);

        // Assert
        verify(rbPublisher).sendCacheSubscribe(anyString(), anyList(), anyList(), anyList(), eq(false));
        assertFalse(acked.get(), "with a cluster subscribe in flight the ack is deferred to its confirmation");
    }
}
