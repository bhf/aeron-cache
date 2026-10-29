package com.bhf.aeroncache.sse.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import lombok.extern.log4j.Log4j2;

/**
 * SSE-flavoured {@link com.bhf.aeroncache.services.cache.subscription.CacheSubscriptionRequestPublisher}.
 * All subscription machinery lives in the shared base; this subclass only adapts the SSE lifecycle
 * callbacks ({@link SSEStatusHandler}) onto the base's session removal.
 */
@Log4j2
public class CacheSubscriptionRequestPublisher<I extends Reusable, K extends Reusable, V extends Reusable>
        extends com.bhf.aeroncache.services.cache.subscription.CacheSubscriptionRequestPublisher<I, K, V>
        implements SSEStatusHandler {

    public CacheSubscriptionRequestPublisher(CacheRequestPublisher rbPublisher) {
        super(rbPublisher);
    }

    @Override
    public void handleSSEError(AeronCache cluster, String requestId, String sseSessionId) {
        removeSession(cluster, requestId, sseSessionId);
    }

    @Override
    public void handleSSEClosed(AeronCache cluster, String requestId, String sseSessionId) {
        removeSession(cluster, requestId, sseSessionId);
    }
}
