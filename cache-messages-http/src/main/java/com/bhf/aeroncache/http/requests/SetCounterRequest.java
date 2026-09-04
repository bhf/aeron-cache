package com.bhf.aeroncache.http.requests;

/**
 * A request to set a counter to a given value.
 *
 * @param key   The key of the counter.
 * @param value The value to set the counter to.
 */
public record SetCounterRequest(String key, Long value) {
}
