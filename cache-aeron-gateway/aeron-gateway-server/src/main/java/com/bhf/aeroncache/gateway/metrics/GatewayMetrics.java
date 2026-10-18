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
}
