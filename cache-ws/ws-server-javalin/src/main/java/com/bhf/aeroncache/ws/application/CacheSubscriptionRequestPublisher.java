package com.bhf.aeroncache.ws.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import lombok.extern.log4j.Log4j2;

/**
 * WebSocket-flavoured {@link com.bhf.aeroncache.services.cache.subscription.CacheSubscriptionRequestPublisher}.
 * All subscription machinery lives in the shared base; this subclass only adapts the WebSocket
 * lifecycle callbacks ({@link WebsocketStatusHandler}) onto the base's session removal.
 */
@Log4j2
public class CacheSubscriptionRequestPublisher<I extends Reusable, K extends Reusable, V extends Reusable>
        extends com.bhf.aeroncache.services.cache.subscription.CacheSubscriptionRequestPublisher<I, K, V>
        implements WebsocketStatusHandler {

    /** Session id for the internal near cache subscription on the WS interface. */
    private static final String NEAR_CACHE_SESSION_ID = "ws-near-cache";

    public CacheSubscriptionRequestPublisher(CacheRequestPublisher rbPublisher) {
        super(rbPublisher);
    }

    /**
     * @param rbPublisher      the request publisher.
     * @param nearCacheManager the local near cache to warm and hydrate from, or {@code null} to disable.
     */
    public CacheSubscriptionRequestPublisher(CacheRequestPublisher rbPublisher, NearCacheManager nearCacheManager) {
        super(rbPublisher, nearCacheManager, NEAR_CACHE_SESSION_ID);
    }

    @Override
    public void handleWsError(AeronCache cluster, String requestId, String wsSessionId) {
        removeSession(cluster, requestId, wsSessionId);
    }

    @Override
    public void handleWsClosed(AeronCache cluster, String requestId, String wsSessionId) {
        removeSession(cluster, requestId, wsSessionId);
    }
}
