package com.bhf.aeroncache.gateway.metrics;

/**
 * Metrics for the Aeron transport gateway.
 * <p>
 * Implementations are called from the gateway's hot path, so they must not block or allocate. Session
 * callbacks may arrive from the Aeron client conductor thread, request and update callbacks from the
 * gateway ingress and cluster egress threads.
 */
public interface GatewayMetrics {

    /**
     * A client connected to the gateway's request channel.
     */
    void onSessionConnected();

    /**
     * A client disconnected from the gateway's request channel.
     */
    void onSessionDisconnected();

    /**
     * A request of the given type was received from a client.
     *
     * @param requestType the kind of request.
     */
    void onRequest(GatewayRequestType requestType);

    /**
     * A streaming subscription update was published to a client.
     */
    void onStreamUpdatePublished();

    /**
     * Record the current subscription state for a cache domain. Unlike the other methods this is a
     * control-plane gauge update, called from the subscription publisher when a client subscribes,
     * unsubscribes or disconnects - not from the per-message hot path.
     * <p>
     * A "subscription" is one client session registered against one cache; "subscribers" is the number
     * of distinct client sessions holding at least one subscription. The internal near-cache warming
     * session is excluded, so these reflect client subscriptions only.
     *
     * @param domain                   the cache domain ({@code cache} or {@code counters}).
     * @param wholeCacheSubscriptions  active whole-cache subscriptions.
     * @param keyedSubscriptions       active keyed (per-key) subscriptions.
     * @param subscribers              distinct client sessions holding at least one subscription.
     */
    void setSubscriptions(String domain, long wholeCacheSubscriptions, long keyedSubscriptions, long subscribers);

    /**
     * Record the current near cache state for a cache domain. Like {@link #setSubscriptions} this is a
     * control-plane gauge update, called from the subscription publisher when a client subscribes,
     * unsubscribes or disconnects, or when a cache is warmed, primed, stopped or deleted - not from the
     * per-message hot path. Only published when the near cache is enabled.
     * <p>
     * These are dedicated near-cache metrics, distinct from {@link #setSubscriptions}: {@code warmCaches}
     * counts caches held warm locally, and the subscriber counts are distinct client sessions (not
     * subscriptions) split by whole-cache vs keyed interest. The internal near-cache warming session is
     * excluded.
     *
     * @param domain               the cache domain ({@code cache} or {@code counters}).
     * @param warmCaches           caches currently kept warm in the near cache.
     * @param wholeCacheSubscribers distinct client sessions subscribed to whole caches.
     * @param keyedSubscribers     distinct client sessions subscribed to specific keys.
     * @param patchSubscribers     distinct client sessions holding a PATCH-mode subscription (whose updates
     *                             are turned into synthetic merge-patch deltas from the warm copy).
     */
    void setNearCache(String domain, long warmCaches, long wholeCacheSubscribers, long keyedSubscribers,
                      long patchSubscribers);

    /**
     * Record a near cache hit: one hydrating subscription on one cache served from the warm local copy
     * (replayed from the near cache, with no cluster snapshot). Called from the gateway ingress thread on
     * the local-replay path, so it increments only when a subscription really was served by the near cache
     * - the hit signal used to prove near-cache serving. Only meaningful when the near cache is enabled.
     *
     * @param domain the cache domain ({@code cache} or {@code counters}).
     * @param keyed  {@code true} for a keyed subscription, {@code false} for a whole-cache one.
     */
    void onNearCacheHydrationServed(String domain, boolean keyed);

    /**
     * Record a non-hydrating subscription served from the near cache: one FULL subscription on one cache
     * that rode the near cache session's existing shared cluster subscription instead of opening a redundant
     * per-subscriber one. Unlike {@link #onNearCacheHydrationServed} no snapshot is replayed (the subscriber
     * did not ask to hydrate); this counts the cluster subscribe that was avoided. Called from the gateway
     * ingress thread on the warm-ride path. Only meaningful when the near cache is enabled.
     *
     * @param domain the cache domain ({@code cache} or {@code counters}).
     * @param keyed  {@code true} for a keyed subscription, {@code false} for a whole-cache one.
     */
    void onNearSubscriptionServed(String domain, boolean keyed);

    /**
     * Record one synthetic patch generation for a near-cache PATCH subscriber. Called from the cluster
     * egress thread on the local patch-emission path, once per subscriber per update, so it increments only
     * when a patch was actually generated for a subscriber. Only meaningful when the near cache is enabled.
     *
     * @param domain  the cache domain ({@code cache} or {@code counters}).
     * @param outcome whether a merge-patch delta was emitted, a full baseline was sent, or a no-op delta
     *                was suppressed.
     */
    void onNearPatch(String domain, NearPatchOutcome outcome);
}
