package com.bhf.aeroncache.ws.bidi;

/**
 * Thrown when an inbound bidirectional websocket frame cannot be decoded: malformed JSON, a missing or
 * unknown {@code type} discriminator, or an unknown {@code op}. The route handler translates this into a
 * {@code BidiError} frame back to the client.
 */
public class BidiProtocolException extends RuntimeException {

    public BidiProtocolException(String message) {
        super(message);
    }

    public BidiProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
