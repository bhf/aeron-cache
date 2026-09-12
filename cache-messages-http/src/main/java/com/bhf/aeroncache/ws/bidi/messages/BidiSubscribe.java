package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Subscribe to streaming updates for one or more caches over the bidirectional websocket protocol.
 *
 * <p>The JSON mirror of the Aeron gateway's {@code GatewaySubscribe}. Each {@link CacheSelector} names a
 * cache, an optional key (a {@code null}/blank key denotes a whole-cache subscription) and a
 * {@link SubscriptionMode}.</p>
 *
 * @param correlationId the client correlation id, echoed on streamed updates for this subscription.
 * @param counters      whether the subscription targets the counters cache flavour.
 * @param sendSnapshot  whether to request initial state hydration.
 * @param caches        the caches/keys to subscribe to.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidiSubscribe(String correlationId,
                            boolean counters,
                            boolean sendSnapshot,
                            List<CacheSelector> caches) implements BidiClientMessage {

    /**
     * A single cache (and optional key) to subscribe to.
     *
     * @param cacheId the cache id.
     * @param key     the key, or {@code null}/blank for a whole-cache subscription.
     * @param mode    the subscription mode; {@code null} is treated as {@link SubscriptionMode#FULL}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CacheSelector(String cacheId, String key, SubscriptionMode mode) {
    }
}
