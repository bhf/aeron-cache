package com.bhf.aeroncache.sse.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.requests.SubscriptionMode;

import java.util.List;
import java.util.function.Consumer;

public interface CacheSubscriptions {
    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheId,
                          String sseSessionId, String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);

    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheId,
                          List<String> keys, SubscriptionMode mode, String sseSessionId, String requestId,
                          boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);
}
