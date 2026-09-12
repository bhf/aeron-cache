package com.bhf.aeroncache.ws.bidi.messages;

import java.util.List;

/**
 * Confirms that a {@link BidiSubscribe} is now live: the cluster has registered the subscription, so
 * updates for the requested caches will be delivered from this point on.
 *
 * <p>Emitted once per subscribe request, correlated via {@code correlationId}. Clients (and tests) can
 * treat receipt of this frame as the point at which it is safe to issue mutations and expect the resulting
 * updates to be streamed back &mdash; removing the need to guess with a fixed delay.</p>
 *
 * @param type          the frame discriminator; always {@link BidiServerMessage#SUBSCRIBED}.
 * @param correlationId the correlation id echoed from the subscribe request.
 * @param cacheIds      the caches the subscription now covers.
 */
public record BidiSubscribeAck(String type,
                               String correlationId,
                               List<String> cacheIds) implements BidiServerMessage {

    /**
     * Build a subscription-confirmed ack frame.
     *
     * @param correlationId the correlation id to echo.
     * @param cacheIds      the caches now subscribed.
     * @return the frame.
     */
    public static BidiSubscribeAck of(String correlationId, List<String> cacheIds) {
        return new BidiSubscribeAck(SUBSCRIBED, correlationId, cacheIds == null ? List.of() : cacheIds);
    }
}
