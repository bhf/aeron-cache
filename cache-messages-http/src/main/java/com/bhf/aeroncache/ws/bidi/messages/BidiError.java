package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * An error correlated to a client frame.
 *
 * <p>The JSON mirror of the gateway's {@code GatewayError}.</p>
 *
 * @param type          the frame discriminator; always {@link BidiServerMessage#ERROR}.
 * @param correlationId the correlation id echoed from the originating frame (may be {@code null} if the
 *                      frame could not be parsed).
 * @param status        the error status.
 * @param message       a human readable error message.
 */
public record BidiError(String type,
                        String correlationId,
                        CacheOperationStatus status,
                        String message) implements BidiServerMessage {

    /**
     * Build an error frame, normalising a {@code null} status to {@link CacheOperationStatus#ERROR}.
     *
     * @param correlationId the correlation id to echo, if known.
     * @param status        the error status.
     * @param message       the error message.
     * @return the frame.
     */
    public static BidiError of(String correlationId, CacheOperationStatus status, String message) {
        return new BidiError(ERROR, correlationId,
                status == null ? CacheOperationStatus.ERROR : status, message);
    }
}
