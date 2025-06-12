package com.bhf.aeroncache.ws.application;

import com.bhf.aeroncache.AeronCache;

/**
 * Handle websocket statuses.
 */
public interface WebsocketStatusHandler {

    /**
     * Handle a websocket error. Removes any associated consumer
     * and cancels cache side subscriptions if necessary.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param wsSessionId The websocket session ID.
     */
    void handleWsError(AeronCache cluster, String requestId, String wsSessionId);

    /**
     * Handle a websocket closing. Removes any associated consumer
     * * and cancels cache side subscriptions if necessary.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param wsSessionId The websocket session ID.
     */
    void handleWsClosed(AeronCache cluster, String requestId, String wsSessionId);
}
