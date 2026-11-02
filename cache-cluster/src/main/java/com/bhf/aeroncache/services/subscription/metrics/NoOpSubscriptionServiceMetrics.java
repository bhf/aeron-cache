package com.bhf.aeroncache.services.subscription.metrics;

/**
 * A {@link SubscriptionServiceMetrics} that records nothing. Used when no media driver counters are
 * wired in, e.g. in unit tests.
 */
public class NoOpSubscriptionServiceMetrics implements SubscriptionServiceMetrics {
    @Override
    public void onWholeCacheSubscription() {

    }

    @Override
    public void onKeySubscription() {

    }

    @Override
    public void onDuplicateSubscription() {

    }

    @Override
    public void onUnsubscription() {

    }

    @Override
    public void onSessionClosed() {

    }

    @Override
    public void onUpdatePublished() {

    }

    @Override
    public void setActiveSubscriptions(long count) {

    }
}
