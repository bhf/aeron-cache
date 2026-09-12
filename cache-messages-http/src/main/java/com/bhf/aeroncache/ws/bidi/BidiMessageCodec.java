package com.bhf.aeroncache.ws.bidi;

import com.bhf.aeroncache.ws.bidi.messages.BidiBulk;
import com.bhf.aeroncache.ws.bidi.messages.BidiClientMessage;
import com.bhf.aeroncache.ws.bidi.messages.BidiCommand;
import com.bhf.aeroncache.ws.bidi.messages.BidiServerMessage;
import com.bhf.aeroncache.ws.bidi.messages.BidiSubscribe;
import com.bhf.aeroncache.ws.bidi.messages.BidiUnsubscribe;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON (de)serialization for the bidirectional websocket protocol.
 *
 * <p>The transport-neutral codec seam for the websocket BIDI endpoint, playing the role the SBE
 * {@code GatewayRequestWriter}/{@code GatewayResponseWriter} play for the Aeron gateway. Inbound frames
 * are dispatched on their {@code type} discriminator to the matching {@link BidiClientMessage} record;
 * outbound {@link BidiServerMessage} frames are serialized to JSON text.</p>
 *
 * <p>Instances are thread-safe: the underlying {@link ObjectMapper} is safe for concurrent use once
 * configured.</p>
 */
public class BidiMessageCodec {

    private static final String TYPE_FIELD = "type";

    private final ObjectMapper mapper;

    public BidiMessageCodec() {
        this(new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    public BidiMessageCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Decode an inbound client frame.
     *
     * @param json the raw JSON text received on the websocket.
     * @return the decoded frame.
     * @throws BidiProtocolException if the JSON is malformed, the {@code type} is missing/unknown, or an
     *                               {@code op} is unknown.
     */
    public BidiClientMessage parse(String json) {
        final JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            throw new BidiProtocolException("Malformed JSON frame", e);
        }
        if (root == null || !root.isObject()) {
            throw new BidiProtocolException("Frame is not a JSON object");
        }
        final JsonNode typeNode = root.get(TYPE_FIELD);
        if (typeNode == null || !typeNode.isTextual()) {
            throw new BidiProtocolException("Frame missing 'type' field");
        }
        final String type = typeNode.asText();
        try {
            return switch (type) {
                case BidiClientMessage.COMMAND -> mapper.treeToValue(root, BidiCommand.class);
                case BidiClientMessage.SUBSCRIBE -> mapper.treeToValue(root, BidiSubscribe.class);
                case BidiClientMessage.UNSUBSCRIBE -> mapper.treeToValue(root, BidiUnsubscribe.class);
                case BidiClientMessage.BULK -> mapper.treeToValue(root, BidiBulk.class);
                default -> throw new BidiProtocolException("Unknown frame type '" + type + "'");
            };
        } catch (BidiProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new BidiProtocolException("Could not decode '" + type + "' frame: " + e.getMessage(), e);
        }
    }

    /**
     * Encode an outbound server frame to JSON text.
     *
     * @param message the frame to serialize.
     * @return the JSON text to send on the websocket.
     * @throws BidiProtocolException if serialization fails.
     */
    public String write(BidiServerMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new BidiProtocolException("Could not encode " + message.type() + " frame", e);
        }
    }
}
