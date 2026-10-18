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
}
