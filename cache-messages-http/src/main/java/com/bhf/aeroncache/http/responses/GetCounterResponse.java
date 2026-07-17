package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from getting a counter value.
 *
 * @param cacheId         The ID of the counter cache.
 * @param key             The key of the counter.
 * @param value           The counter value.
 * @param operationStatus The status of the operation.
 */
public record GetCounterResponse(String cacheId, String key, Long value, CacheOperationStatus operationStatus) {
}
