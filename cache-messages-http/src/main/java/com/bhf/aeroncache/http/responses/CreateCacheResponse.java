package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from creating a cache.
 * @param cacheId The ID of the cache.
 * @param operationStatus The status of the operation.
 */
public record CreateCacheResponse(String cacheId, CacheOperationStatus operationStatus) {

}
