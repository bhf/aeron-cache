package com.bhf.aeroncache.http.responses;

/**
 * A counter item.
 *
 * @param key   The key of the counter.
 * @param value The counter value.
 */
public record CounterItem(String key, Long value) {
}
