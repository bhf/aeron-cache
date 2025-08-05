package com.bhf.aeroncache.sse.application;

import com.bhf.aeroncache.AeronCache;

/**
 * Handle SSE statuses.
 */
public interface SSEStatusHandler {

    /**
     * Handle an SSE error. Removes any associated consumer
     * and cancels cache side subscriptions if necessary.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param sseSessionId The SSE session ID.
     */
    void handleSSEError(AeronCache cluster, String requestId, String sseSessionId);

    /**
     * Handle SSE closing. Removes any associated consumer
     * * and cancels cache side subscriptions if necessary.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param sseSessionId The SSE session ID.
     */
    void handleSSEClosed(AeronCache cluster, String requestId, String sseSessionId);
}
