package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from deleting a cache.
 *
 * @param cacheId         The Cache ID.
 * @param operationStatus The status of the operation.
 */
public record DeleteCacheResponse(String cacheId, CacheOperationStatus operationStatus) {

}
