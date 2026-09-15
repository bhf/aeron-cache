package com.bhf.aeroncache.gateway.client;

/**
 * A single pending TTL removal timer delivered to a {@link GatewayClientListener}.
 *
 * @param timerType the type of timer, either {@code CACHE} or {@code COUNTER}.
 * @param cacheId   the cache (or counter cache) the entry will be removed from.
 * @param key       the key that will be removed.
 * @param deadline  the epoch time (millis) at which the removal is scheduled to fire.
 */
public record GatewayTimer(String timerType, String cacheId, String key, long deadline) {
}
