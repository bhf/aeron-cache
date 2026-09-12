package com.bhf.aeroncache.ws.bidi.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A request/response cache command sent over the bidirectional websocket protocol.
 *
 * <p>The JSON mirror of the Aeron gateway's {@code GatewayCommand}. {@link #op()} identifies the operation
 * (mapping onto {@link WsOp#msgType()}); the remaining fields carry its arguments. {@code ttl} and
 * {@code counterValue} default to {@code 0} when omitted.</p>
 *
 * @param correlationId the client correlation id, echoed on the response.
 * @param op            the command operation.
 * @param cacheId       the target cache id.
 * @param key           the entry key, where applicable.
 * @param value         the entry value, where applicable.
 * @param ttl           the entry time-to-live in millis, where applicable ({@code 0} for none).
 * @param counterValue  the counter amount/value, for counter operations ({@code 0} when omitted).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidiCommand(String correlationId,
                          WsOp op,
                          String cacheId,
                          String key,
                          String value,
                          long ttl,
                          long counterValue) implements BidiClientMessage {
}
