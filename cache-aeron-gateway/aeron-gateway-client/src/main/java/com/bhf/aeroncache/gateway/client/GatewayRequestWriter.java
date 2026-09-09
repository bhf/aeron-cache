package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.BooleanType;
import com.bhf.aeroncache.gateway.messages.GatewayCommandEncoder;
import com.bhf.aeroncache.gateway.messages.GatewaySubscribeEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayUnsubscribeEncoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderEncoder;
import com.bhf.aeroncache.gateway.messages.SubscriptionMode;
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
     * Encode a subscribe frame (whole-cache, full mode).
     *
     * @return the encoded length in bytes.
     */
    int encodeSubscribe(MutableDirectBuffer buffer, String correlationId, List<String> cacheIds,
                        boolean sendSnapshot, boolean counters) {
        return encodeSubscribe(buffer, correlationId, cacheIds, null, false, sendSnapshot, counters);
    }

    /**
     * Encode a subscribe with per-cache keys and a subscription mode.
     *
     * @param keys  The keys parallel to {@code cacheIds}; a {@code null} entry (or {@code null} list) denotes a
     *              whole-cache subscription for that cache.
     * @param patch {@code true} to subscribe in patch mode, {@code false} for full mode.
     * @return the encoded length in bytes.
     */
    int encodeSubscribe(MutableDirectBuffer buffer, String correlationId, List<String> cacheIds, List<String> keys,
                        boolean patch, boolean sendSnapshot, boolean counters) {
        final var sbeMode = patch ? SubscriptionMode.PATCH : SubscriptionMode.FULL;
        var subscribeEnc = subscribeEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder)
                .sendSnapshot(mapBoolean(sendSnapshot))
                .counters(mapBoolean(counters));
        var group = subscribeEnc.cacheIdsCount(cacheIds.size());
        for (int i = 0; i < cacheIds.size(); i++) {
            final var key = (keys == null) ? null : keys.get(i);
            group.next()
                    .mode(sbeMode)
                    .cacheId(nullSafe(cacheIds.get(i)))
                    .key(key == null ? "" : key);
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
