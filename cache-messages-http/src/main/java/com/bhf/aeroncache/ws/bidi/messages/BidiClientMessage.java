package com.bhf.aeroncache.ws.bidi.messages;

/**
 * Marker for a frame sent by a client to the gateway over the bidirectional websocket protocol.
 *
 * <p>Concrete frames are discriminated by the {@code type} JSON field and decoded by
 * {@code BidiMessageCodec}: {@code "command"} ({@link BidiCommand}), {@code "subscribe"}
 * ({@link BidiSubscribe}) and {@code "unsubscribe"} ({@link BidiUnsubscribe}). This mirrors the three
 * client-facing message types on the Aeron gateway ({@code GatewayCommand}/{@code GatewaySubscribe}/
 * {@code GatewayUnsubscribe}).</p>
 */
public sealed interface BidiClientMessage permits BidiCommand, BidiSubscribe, BidiUnsubscribe {

    /** The {@code type} discriminator value for a command frame. */
    String COMMAND = "command";
    /** The {@code type} discriminator value for a subscribe frame. */
    String SUBSCRIBE = "subscribe";
    /** The {@code type} discriminator value for an unsubscribe frame. */
    String UNSUBSCRIBE = "unsubscribe";

    /**
     * The correlation id the client supplied; echoed on every response correlated to this frame.
     *
     * @return the correlation id, or {@code null} if the client omitted one.
     */
    String correlationId();
}
