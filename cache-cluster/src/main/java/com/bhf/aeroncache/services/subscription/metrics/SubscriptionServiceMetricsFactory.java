package com.bhf.aeroncache.services.subscription.metrics;

/**
 * Creates a {@link SubscriptionServiceMetrics} per subscription type. The cluster service holds one
 * factory and asks it for one metrics instance per subscription service it runs, so each gets its own
 * distinctly labelled counters.
 */
public interface SubscriptionServiceMetricsFactory {

    /**
     * Create the metrics for a subscription service.
     *
     * @param subscriptionType identifies the subscription service (e.g. {@code cache}, {@code patch}
     *                         or {@code counters}); used to distinguish the counters and must not
     *                         contain a hyphen.
     * @return the metrics to record against; never {@code null}.
     */
    SubscriptionServiceMetrics create(String subscriptionType);
}
