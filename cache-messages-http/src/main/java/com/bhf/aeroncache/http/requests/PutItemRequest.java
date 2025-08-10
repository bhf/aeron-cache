package com.bhf.aeroncache.http.requests;

/**
 * A request to put an item into the cache.
 *
 * @param cacheId The Id of the cache to put the item into.
 * @param key The key of the item.
 * @param value The value of the item.
 */
public record PutItemRequest(String cacheId, String key, String value) {
}
