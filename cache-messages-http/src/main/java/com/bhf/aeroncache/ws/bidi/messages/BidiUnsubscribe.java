package com.bhf.aeroncache.ws.bidi.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Unsubscribe from streaming updates for a cache over the bidirectional websocket protocol.
 *
 * <p>The JSON mirror of the Aeron gateway's {@code GatewayUnsubscribe}.</p>
 *
 * @param correlationId the client correlation id, echoed on any response.
 * @param counters      whether the subscription targets the counters cache flavour.
 * @param cacheId       the cache id to unsubscribe from.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidiUnsubscribe(String correlationId,
                              boolean counters,
                              String cacheId) implements BidiClientMessage {
}
