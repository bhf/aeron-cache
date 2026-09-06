package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.BooleanType;
import com.bhf.aeroncache.gateway.messages.GatewayCommandEncoder;
import com.bhf.aeroncache.gateway.messages.GatewaySubscribeEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayUnsubscribeEncoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderEncoder;
import org.agrona.MutableDirectBuffer;

import java.util.List;

/**
 * Encodes gateway request frames (command / subscribe / unsubscribe) into a buffer.
 * <p>
 * Not thread safe: a single instance reuses its encoders, so access must be externally
 * serialised. {@link GatewayClient} holds one instance per calling thread (thread-local) so each
 * writer is only ever touched by a single thread.
 */
class GatewayRequestWriter {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final GatewayCommandEncoder commandEncoder = new GatewayCommandEncoder();
    private final GatewaySubscribeEncoder subscribeEncoder = new GatewaySubscribeEncoder();
    private final GatewayUnsubscribeEncoder unsubscribeEncoder = new GatewayUnsubscribeEncoder();

    /**
     * Encode a command frame.
     *
     * @return the encoded length in bytes.
     */
    int encodeCommand(MutableDirectBuffer buffer, int msgType, long ttl, long counterValue,
                      String correlationId, String cacheId, String key, String value) {
        commandEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .msgType(msgType)
                .ttl(ttl)
                .counterValue(counterValue)
                .correlationId(nullSafe(correlationId))
                .cacheId(nullSafe(cacheId))
                .key(nullSafe(key))
                .value(nullSafe(value));
        return commandEncoder.limit();
    }

    /**
     * Encode a subscribe frame.
     *
     * @return the encoded length in bytes.
     */
    int encodeSubscribe(MutableDirectBuffer buffer, String correlationId, List<String> cacheIds,
                        boolean sendSnapshot, boolean counters) {
        var subscribeEnc = subscribeEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .sendSnapshot(mapBoolean(sendSnapshot))
                .counters(mapBoolean(counters));
        var group = subscribeEnc.cacheIdsCount(cacheIds.size());
        for (String cacheId : cacheIds) {
            group.next().cacheId(nullSafe(cacheId));
        }
        subscribeEnc.correlationId(nullSafe(correlationId));
        return subscribeEncoder.limit();
    }

    /**
     * Encode an unsubscribe frame.
     *
     * @return the encoded length in bytes.
     */
    int encodeUnsubscribe(MutableDirectBuffer buffer, String correlationId, String cacheId, boolean counters) {
        unsubscribeEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .counters(mapBoolean(counters))
                .correlationId(nullSafe(correlationId))
                .cacheId(nullSafe(cacheId));
        return unsubscribeEncoder.limit();
    }

    private static BooleanType mapBoolean(boolean value) {
        return value ? BooleanType.T : BooleanType.F;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
