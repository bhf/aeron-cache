package com.bhf.aeroncache.models.responses;

/**
 * The response from getting an item from the cache.
 *
 * @param cacheId The ID of the cache.
 * @param key The key of the item.
 * @param value The value of the item.
 */
public record GetItemResponse(long cacheId, String key, String value) {
}
