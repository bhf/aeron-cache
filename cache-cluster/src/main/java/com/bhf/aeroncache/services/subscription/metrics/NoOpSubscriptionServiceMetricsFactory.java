package com.bhf.aeroncache.services.subscription.metrics;

/**
 * A {@link SubscriptionServiceMetricsFactory} that hands out {@link NoOpSubscriptionServiceMetrics}.
 * This is the default when no media driver counters are wired in, e.g. in unit tests.
 */
public class NoOpSubscriptionServiceMetricsFactory implements SubscriptionServiceMetricsFactory {

    private final SubscriptionServiceMetrics metrics = new NoOpSubscriptionServiceMetrics();

    @Override
    public SubscriptionServiceMetrics create(String subscriptionType) {
        return metrics;
    }
}
