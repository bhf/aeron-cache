package com.bhf.aeroncache.http.requests;

/**
 * A request to increment a counter value.
 *
 * @param key    The key of the counter.
 * @param amount The amount to increment the counter by.
 */
public record IncrementCounterRequest(String key, Long amount) {
}
