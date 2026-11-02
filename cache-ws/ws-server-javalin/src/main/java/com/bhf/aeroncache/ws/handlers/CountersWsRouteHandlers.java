package com.bhf.aeroncache.ws.handlers;

import com.bhf.aeroncache.metrics.WebGatewayMetrics;
import com.bhf.aeroncache.ws.application.CacheSubscriptionRequestPublisher;
import lombok.extern.log4j.Log4j2;

/**
 * Websocket subscription handlers for counter caches. Patch mode subscriptions are not permitted.
 */
@Log4j2
public class CountersWsRouteHandlers extends AbstractWsRouteHandlers {

    public CountersWsRouteHandlers(CacheSubscriptionRequestPublisher subscriptionService, WebGatewayMetrics metrics) {
        super(subscriptionService, false, "counters cache", metrics, "subscribeCounter");
    }
}
