package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.requests.SubscriptionMode;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fan-out subscription service for the gateway. A single cluster subscription per
 * cache is shared across all connected gateway client sessions.
 */
public interface GatewaySubscriptions {

    /**
     * Subscribe a gateway client session to streaming updates for the given caches.
     *
     * @param cluster                    The cluster to use.
     * @param subscriptionFailureHandler The handler for subscription failures.
     * @param cacheIds                   The caches to subscribe to.
     * @param sessionId                  The gateway client session ID (image correlation id).
     * @param requestId                  The request ID.
     * @param sendSnapshot               Whether to request initial state hydration.
     * @param consumer                   The consumer of {@link CacheUpdateEvent}.
     */
    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheIds,
                          String sessionId, String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);

    /**
     * Subscribe a gateway client session to streaming updates for specific keys and a specific mode.
     *
     * @param cluster                    The cluster to use.
     * @param subscriptionFailureHandler The handler for subscription failures.
     * @param cacheIds                   The caches to subscribe to (parallel to {@code keys}).
     * @param keys                       The keys parallel to {@code cacheIds}; a {@code null} entry (or {@code null}
     *                                   list) denotes a whole-cache subscription for that cache.
     * @param mode                       The subscription mode ({@link SubscriptionMode#FULL} or {@link SubscriptionMode#PATCH}).
     * @param sessionId                  The gateway client session ID (image correlation id).
     * @param requestId                  The request ID.
     * @param sendSnapshot               Whether to request initial state hydration.
     * @param consumer                   The consumer of {@link CacheUpdateEvent}.
     */
    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheIds,
                          List<String> keys, SubscriptionMode mode, String sessionId, String requestId,
                          boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);
}
