package com.bhf.aeroncache.ws.handlers;

import com.bhf.aeroncache.ws.application.CacheSubscriptionRequestPublisher;
import lombok.extern.log4j.Log4j2;

/**
 * Websocket subscription handlers for regular caches. Patch mode subscriptions are permitted.
 */
@Log4j2
public class CacheWsRouteHandlers extends AbstractWsRouteHandlers {

    public CacheWsRouteHandlers(CacheSubscriptionRequestPublisher subscriptionService) {
        super(subscriptionService, true, "cache");
    }
}
