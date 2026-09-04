package com.bhf.aeroncache.http.requests;

/**
 * A request to put a timed counter value.
 *
 * @param key   The key of the counter.
 * @param value The counter value.
 * @param ttl   The time to live in milliseconds.
 */
public record PutTimedCounterRequest(String key, Long value, long ttl) {
}
