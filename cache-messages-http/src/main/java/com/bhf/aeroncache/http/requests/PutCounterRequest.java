package com.bhf.aeroncache.http.requests;

/**
 * A request to put a counter value.
 *
 * @param key   The key of the counter.
 * @param value The counter value.
 */
public record PutCounterRequest(String key, Long value) {
}
