package com.bhf.aeroncache.services.cache.subscription;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.requests.SubscriptionMode;

import java.util.List;
import java.util.function.Consumer;

/**
 * Transport-agnostic cache subscription API. Implemented by {@link CacheSubscriptionRequestPublisher}
 * and used by the streaming interfaces (WebSocket, SSE) and the HTTP near cache to subscribe a client
 * session to cache update events.
 */
public interface CacheSubscriptions {

    /**
     * Subscribe a session to whole-cache updates for the given caches.
     *
     * @param cluster                    The cluster to use.
     * @param subscriptionFailureHandler The handler for subscription failures.
     * @param cacheId                    The caches to subscribe to.
     * @param sessionId                  The client session ID.
     * @param requestId                  The request ID.
     * @param sendSnapshot               Whether to request initial state hydration.
     * @param consumer                   The consumer of {@link CacheUpdateEvent}.
     */
    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheId,
                          String sessionId, String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);

    /**
     * Subscribe to cache updates for specific keys and a specific subscription mode.
     *
     * @param subscriptionAckHandler run once when the subscription is confirmed live at the cluster (or
     *                               immediately when it was already live), signalling that mutations issued
     *                               from now on will be streamed back.
     */
    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, Runnable subscriptionAckHandler,
                          List<String> cacheId, List<String> keys, SubscriptionMode mode, String sessionId,
                          String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);
}
