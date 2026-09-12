package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;

/**
 * A streaming cache update pushed to a subscribed client.
 *
 * <p>The JSON mirror of the gateway's {@code GatewayStreamUpdate}. Built from the shared
 * {@link CacheUpdateEvent} the subscription machinery already produces, so the streaming payload is
 * consistent with the existing one-directional websocket routes.</p>
 *
 * @param type          the frame discriminator; always {@link BidiServerMessage#STREAM_UPDATE}.
 * @param correlationId the subscription's correlation id (the event's request id).
 * @param eventType     the type of update.
 * @param cacheId       the cache the update belongs to.
 * @param key           the entry key, where applicable.
 * @param value         the entry value, where applicable.
 */
public record BidiStreamUpdate(String type,
                               String correlationId,
                               CacheUpdateEvent.EventType eventType,
                               String cacheId,
                               String key,
                               String value) implements BidiServerMessage {

    /**
     * Build a stream-update frame from a {@link CacheUpdateEvent}.
     *
     * @param event the update event produced by the subscription machinery.
     * @return the frame.
     */
    public static BidiStreamUpdate from(CacheUpdateEvent<?> event) {
        var value = event.itemValue() == null ? null : String.valueOf(event.itemValue());
        return new BidiStreamUpdate(STREAM_UPDATE, event.requestId(), event.eventType(),
                event.cacheId(), event.itemKey(), value);
    }
}
