package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from getting an item from the cache.
 *
 * @param cacheId         The ID of the cache.
 * @param key             The key of the item.
 * @param value           The value of the item.
 * @param operationStatus The status of the operation.
 */
public record GetItemResponse(String cacheId, String key, String value, CacheOperationStatus operationStatus) {
}
