package com.bhf.aeroncache.http.responses;

/**
 * Response from putting an item into a cache.
 *
 * @param cacheId The cache the item was put into.
 * @param key The key under which the item was added.
 */
public record PutItemResponse(long cacheId, String key) {
}
