package com.bhf.aeroncache.http.requests;

/**
 * A request to get an item from the cache.
 *
 * @param cacheId The ID of the cache.
 * @param key The key of the item to get.
 */
public record GetItemRequest(long cacheId, String key) {
}
