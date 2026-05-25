package com.bhf.aeroncache.http.requests;

/**
 * A request to put a timed item into the cache.
 *
 * @param key The key of the item.
 * @param value The value of the item.
 * @param ttl The time to live in milliseconds.
 */
public record PutTimedItemRequest(String key, String value, long ttl) {
}
