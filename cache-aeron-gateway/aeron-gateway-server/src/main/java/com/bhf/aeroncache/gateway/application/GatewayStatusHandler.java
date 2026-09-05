package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.AeronCache;

/**
 * Handle gateway client session lifecycle events. Removes any associated
 * consumers and cancels cluster side subscriptions when necessary.
 */
public interface GatewayStatusHandler {

    /**
     * Handle a gateway client session closing (its Aeron image becoming unavailable).
     * Removes any associated consumer and cancels cluster side subscriptions if necessary.
     *
     * @param cluster   The cluster to use.
     * @param requestId The request ID.
     * @param sessionId The gateway client session ID.
     */
    void handleClosed(AeronCache cluster, String requestId, String sessionId);
}
