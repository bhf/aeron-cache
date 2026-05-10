package com.bhf.aeroncache.http.requests;

/**
 * A request to put an item into the cache.
 *
 * @param key The key of the item.
 * @param value The value of the item.
 */
public record PutItemRequest(String key, String value) {
}
