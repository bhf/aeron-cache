package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;

/**
 * A reusable, gateway-local carrier for a single streaming cache update, replacing the immutable
 * {@link CacheUpdateEvent} record on the gateway's dispatch path.
 * <p>
 * A streaming update fans out to every subscriber of a cache, so allocating a {@code CacheUpdateEvent}
 * per subscriber (as the shared record path does) is the highest-frequency allocation in the gateway. The
 * shared record cannot itself be reused because ws/sse/near consumers retain it, but within the gateway an
 * update never escapes the single cluster egress thread: {@link GatewaySubscriptionPublisher} populates one
 * of these, hands it to each session's consumer, and each consumer encodes it to its response publication
 * synchronously before the next dispatch. A single instance per publisher, reused across subscribers and
 * across updates, is therefore safe and allocation-free.
 * <p>
 * Not thread safe: only ever touched by the cluster egress thread that owns its publisher.
 */
@Flyweight
public class GatewayStreamUpdate {

    private String cacheId;
    private CacheUpdateEvent.EventType eventType;
    private String key;
    private Object value;
    private String requestId;

    /**
     * Populate this update in place before dispatching it to subscribers.
     */
    public void set(String cacheId, CacheUpdateEvent.EventType eventType, String key, Object value, String requestId) {
        this.cacheId = cacheId;
        this.eventType = eventType;
        this.key = key;
        this.value = value;
        this.requestId = requestId;
    }

    public String cacheId() {
        return cacheId;
    }

    public CacheUpdateEvent.EventType eventType() {
        return eventType;
    }

    public String key() {
        return key;
    }

    public Object value() {
        return value;
    }

    public String requestId() {
        return requestId;
    }
}
