package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from incrementing a counter value.
 *
 * @param cacheId         The ID of the counter cache.
 * @param key             The key of the counter.
 * @param value           The counter value after incrementing.
 * @param operationStatus The status of the operation.
 */
public record IncrementCounterResponse(String cacheId, String key, Long value, CacheOperationStatus operationStatus) {
}
