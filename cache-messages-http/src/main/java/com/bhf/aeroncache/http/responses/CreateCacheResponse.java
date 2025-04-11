package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.messages.OperationStatus;

/**
 * The response from creating a cache.
 * @param cacheId The ID of the cache.
 * @param operationStatus The status of the operation.
 */
public record CreateCacheResponse(long cacheId, OperationStatus operationStatus) {

}
