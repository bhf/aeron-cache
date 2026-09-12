package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response to a {@link BidiCommand}, correlated via {@code correlationId}.
 *
 * <p>The JSON mirror of the gateway's {@code GatewayCommandResponse}. Counter operations report their
 * resulting value in {@link #value()} as a string.</p>
 *
 * @param type          the frame discriminator; always {@link BidiServerMessage#COMMAND_RESPONSE}.
 * @param correlationId the correlation id echoed from the originating command.
 * @param status        the operation status.
 * @param cacheId       the cache the command targeted.
 * @param key           the entry key, where applicable.
 * @param value         the entry value, where applicable.
 */
public record BidiCommandResponse(String type,
                                  String correlationId,
                                  CacheOperationStatus status,
                                  String cacheId,
                                  String key,
                                  String value) implements BidiServerMessage {

    /**
     * Build a command-response frame, normalising a {@code null} status to {@link CacheOperationStatus#NONE}.
     *
     * @param correlationId the correlation id to echo.
     * @param status        the operation status.
     * @param cacheId       the target cache id.
     * @param key           the entry key, where applicable.
     * @param value         the entry value, where applicable.
     * @return the frame.
     */
    public static BidiCommandResponse of(String correlationId, CacheOperationStatus status,
                                         String cacheId, String key, String value) {
        return new BidiCommandResponse(COMMAND_RESPONSE, correlationId,
                status == null ? CacheOperationStatus.NONE : status, cacheId, key, value);
    }
}
