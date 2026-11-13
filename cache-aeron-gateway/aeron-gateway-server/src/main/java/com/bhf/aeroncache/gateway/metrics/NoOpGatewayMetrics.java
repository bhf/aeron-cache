package com.bhf.aeroncache.gateway.metrics;

/**
 * A {@link GatewayMetrics} that records nothing.
 */
public class NoOpGatewayMetrics implements GatewayMetrics {
    @Override
    public void onSessionConnected() {

    }

    @Override
    public void onSessionDisconnected() {

    }

    @Override
    public void onRequest(GatewayRequestType requestType) {

    }

    @Override
    public void onStreamUpdatePublished() {

    }

    @Override
    public void setSubscriptions(String domain, long wholeCacheSubscriptions, long keyedSubscriptions, long subscribers) {

    }

    @Override
    public void setNearCache(String domain, long warmCaches, long wholeCacheSubscribers, long keyedSubscribers,
                             long patchSubscribers) {

    }

    @Override
    public void onNearCacheHydrationServed(String domain, boolean keyed) {

    }

    @Override
    public void onNearSubscriptionServed(String domain, boolean keyed) {

    }

    @Override
    public void onNearPatch(String domain, NearPatchOutcome outcome) {

    }
}
