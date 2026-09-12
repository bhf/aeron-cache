package com.bhf.aeroncache.http.responses;

import java.util.List;

/**
 * Confirms that a streaming subscription is now live: the cluster has registered it, so updates for the
 * requested caches will be delivered from this point on.
 *
 * <p>Sent once per subscription over the one-directional websocket and SSE streaming endpoints. It is
 * distinguished from a {@link CacheUpdateEvent} by the {@code type} field (data events carry
 * {@code eventType} instead), so existing clients that only consume update events can ignore it. Tests use
 * it as a readiness barrier in place of a fixed delay.</p>
 *
 * @param type     the frame discriminator; always {@code "subscribed"}.
 * @param cacheIds the caches the subscription now covers.
 * @param requestId the request id associated with the subscription.
 */
public record SubscriptionAck(String type, List<String> cacheIds, String requestId) {

    /** The {@code type} discriminator value for a subscription-confirmed ack frame. */
    public static final String SUBSCRIBED = "subscribed";

    /**
     * Build a subscription-confirmed ack.
     *
     * @param cacheIds  the caches now subscribed.
     * @param requestId the request id.
     * @return the ack.
     */
    public static SubscriptionAck of(List<String> cacheIds, String requestId) {
        return new SubscriptionAck(SUBSCRIBED, cacheIds == null ? List.of() : cacheIds, requestId);
    }
}
