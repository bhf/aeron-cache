package com.bhf.aeroncache.ws.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.requests.SubscriptionMode;

import java.util.List;
import java.util.function.Consumer;

public interface CacheSubscriptions {
    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheId,
                          String wsSessionId, String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);

    /**
     * Subscribe to cache updates.
     *
     * @param subscriptionAckHandler run once when the subscription is confirmed live at the cluster (or
     *                               immediately when it was already live), signalling that mutations issued
     *                               from now on will be streamed back.
     */
    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, Runnable subscriptionAckHandler,
                          List<String> cacheId, List<String> keys, SubscriptionMode mode, String wsSessionId,
                          String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);
}
