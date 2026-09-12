package com.bhf.aeroncache.ws.bidi.messages;

/**
 * Marker for a frame sent by the gateway to a client over the bidirectional websocket protocol.
 *
 * <p>Every frame carries a {@code type} discriminator and (except purely informational cases) the
 * {@code correlationId} echoed from the originating client frame. Concrete frames mirror the five
 * gateway-to-client message types on the Aeron gateway: {@link BidiCommandResponse},
 * {@link BidiStreamUpdate}, {@link BidiError}, {@link BidiEntries} and {@link BidiStats}.</p>
 */
public sealed interface BidiServerMessage
        permits BidiCommandResponse, BidiStreamUpdate, BidiError, BidiEntries, BidiStats, BidiSubscribeAck {

    /** The {@code type} discriminator value for a command-response frame. */
    String COMMAND_RESPONSE = "commandResponse";
    /** The {@code type} discriminator value for a streaming-update frame. */
    String STREAM_UPDATE = "streamUpdate";
    /** The {@code type} discriminator value for an error frame. */
    String ERROR = "error";
    /** The {@code type} discriminator value for an entries batch frame. */
    String ENTRIES = "entries";
    /** The {@code type} discriminator value for a stats batch frame. */
    String STATS = "stats";
    /** The {@code type} discriminator value for a subscription-confirmed ack frame. */
    String SUBSCRIBED = "subscribed";

    /**
     * The discriminator identifying this frame's concrete type.
     *
     * @return the {@code type} field value.
     */
    String type();
}
