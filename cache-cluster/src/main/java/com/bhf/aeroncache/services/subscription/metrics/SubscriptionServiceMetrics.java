package com.bhf.aeroncache.services.subscription.metrics;

/**
 * Metrics for a {@link com.bhf.aeroncache.services.subscription.CacheSubscriptionService}.
 * <p>
 * One instance is held per subscription service (the cluster service runs three: the main cache, the
 * patch stream and the counters cache), so an implementation records against a single subscription
 * "type". Implementations are called from the cluster service's single processing thread on the hot
 * path, so they must not block or allocate.
 */
public interface SubscriptionServiceMetrics {

    /**
     * A session newly subscribed to a whole cache.
     */
    void onWholeCacheSubscription();

    /**
     * A session newly subscribed to a single key within a cache.
     */
    void onKeySubscription();

    /**
     * A subscribe request was received for a session that was already subscribed to the target.
     */
    void onDuplicateSubscription();

    /**
     * A session was removed from a subscription by an explicit unsubscribe request.
     */
    void onUnsubscription();

    /**
     * A client session closed and its subscriptions were cleaned up.
     */
    void onSessionClosed();

    /**
     * An update was published to a subscribed session (one offer to one session).
     */
    void onUpdatePublished();

    /**
     * Record the current number of active subscriptions, where a subscription is one session
     * registered against one subscription target (a whole cache or a key).
     *
     * @param count the current number of active subscriptions.
     */
    void setActiveSubscriptions(long count);
}
