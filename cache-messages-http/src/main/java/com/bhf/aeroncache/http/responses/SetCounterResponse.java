package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from setting a counter value.
 *
 * @param cacheId         The ID of the counter cache.
 * @param key             The key of the counter.
 * @param value           The counter value after setting.
 * @param operationStatus The status of the operation.
 */
public record SetCounterResponse(String cacheId, String key, Long value, CacheOperationStatus operationStatus) {
}
