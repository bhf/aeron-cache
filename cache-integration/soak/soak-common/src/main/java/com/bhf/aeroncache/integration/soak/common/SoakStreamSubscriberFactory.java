package com.bhf.aeroncache.integration.soak.common;

/**
 * Opens {@link SoakStreamSubscriber}s for a URL-based streaming transport. A concrete factory (WebSocket or
 * SSE) knows the transport's scheme, host, port and route templates and turns a {@link SubscriptionSpec} into a
 * connection that is already subscribed and buffering updates. This is the single seam that lets the shared
 * {@link UrlStreamingSoakWorkload} drive the same coverage matrix over either transport.
 */
public interface SoakStreamSubscriberFactory {

    /** Opens a connection for the given subscription and begins buffering its updates immediately. */
    SoakStreamSubscriber open(SubscriptionSpec spec);
}
