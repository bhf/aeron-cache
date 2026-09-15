package com.bhf.aeroncache.http.responses;

/**
 * A single pending TTL removal timer.
 *
 * @param timerType The type of timer, either {@code CACHE} or {@code COUNTER}.
 * @param cacheId   The ID of the cache (or counter cache) the entry will be removed from.
 * @param key       The key that will be removed.
 * @param deadline  The epoch time (millis) at which the removal is scheduled to fire.
 */
public record TimerInfo(String timerType, String cacheId, String key, long deadline) {
}
