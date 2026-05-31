package com.bhf.aeroncache.sse.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;

import java.util.function.Consumer;

public interface CacheSubscriptions {
    void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, String cacheId,
                          String sseSessionId, String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer);
}
