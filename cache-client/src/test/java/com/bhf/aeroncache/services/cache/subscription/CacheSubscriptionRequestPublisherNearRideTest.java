package com.bhf.aeroncache.services.cache.subscription;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.events.CacheUpdateEvent;
import com.bhf.aeroncache.metrics.WebGatewayMetrics;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Covers the near-cache "warm ride" for non-hydrating subscriptions on the shared subscription publisher used
 * by the SSE and WS gateways: a non-hydrating FULL subscriber on an already-warm cache must ride the near
 * cache session's existing shared cluster subscription instead of opening a redundant per-subscriber one,
 * while cold caches, PATCH mode and the internal near-cache session still subscribe to the cluster.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheSubscriptionRequestPublisher - non-hydrating near cache ride")
class CacheSubscriptionRequestPublisherNearRideTest {

    private static final String NEAR_SESSION = "near-cache";
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

    private static final String DOMAIN = "cache";

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final Consumer<Void> failureHandler = v -> { };
    private final Consumer<CacheUpdateEvent> consumer = e -> { };

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CacheSubscriptionRequestPublisher newPublisherWithNearCache() {
        var publisher = new CacheSubscriptionRequestPublisher(rbPublisher, nearCacheManager, NEAR_SESSION);
        publisher.registerMetrics(meterRegistry, DOMAIN);
        return publisher;
    }

    private void verifyNoClusterSubscribe() {
        verify(rbPublisher, never()).sendCacheSubscribe(anyString(), anyList(), anyList(), anyList(), anyBoolean());
    }

    private double subscriptionsServed(String type) {
        var counter = meterRegistry.find(WebGatewayMetrics.NEAR_SUBSCRIPTIONS_SERVED)
                .tags("domain", DOMAIN, "type", type).counter();
        return counter == null ? 0d : counter.count();
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
        assertEquals(1d, subscriptionsServed("whole"), "warm whole-cache ride should be counted");
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
        assertEquals(1d, subscriptionsServed("keyed"), "warm keyed ride should be counted");
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
        assertEquals(0d, subscriptionsServed("whole"), "a cold cache is not served from the near cache");
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
        assertEquals(0d, subscriptionsServed("whole"), "PATCH is not served from the near cache");
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
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    @DisplayName("With the near cache disabled, a non-hydrating subscribe always subscribes to the cluster")
    void nearCacheDisabledAlwaysSubscribesToCluster() {
        // Arrange
        var sut = new CacheSubscriptionRequestPublisher(rbPublisher);
        var acked = new AtomicBoolean(false);

        // Act
        sut.subscribeToCache(cluster, failureHandler, () -> acked.set(true), List.of(CACHE), null,
                SubscriptionMode.FULL, CLIENT_SESSION, REQUEST_ID, false, consumer);

        // Assert
        verify(rbPublisher).sendCacheSubscribe(anyString(), anyList(), anyList(), anyList(), eq(false));
        assertFalse(acked.get(), "with a cluster subscribe in flight the ack is deferred to its confirmation");
    }
}
