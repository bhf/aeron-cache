package com.bhf.aeroncache.ws.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import io.javalin.websocket.WsContext;

import java.util.function.Consumer;

public interface CacheSubscriptions {
    void subscribeToCache(AeronCache cluster, WsContext wsContext, long cacheId, String wsSessionId, String requestId, Consumer<CacheUpdateEvent> consumer);
}
