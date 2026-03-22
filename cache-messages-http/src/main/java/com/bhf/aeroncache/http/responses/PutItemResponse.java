package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * Response from putting an item into a cache.
 *
 * @param cacheId         The cache the item was put into.
 * @param key             The key under which the item was added.
 * @param operationStatus The status of the operation.
 */
public record PutItemResponse(String cacheId, String key, CacheOperationStatus operationStatus) {
}
