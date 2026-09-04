package com.bhf.aeroncache.http.requests;

/**
 * A request to decrement a counter value.
 *
 * @param key    The key of the counter.
 * @param amount The amount to decrement the counter by.
 */
public record DecrementCounterRequest(String key, Long amount) {
}
