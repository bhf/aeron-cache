package com.bhf.aeroncache.http.responses;

/**
 * The response from creating a cache.
 * @param cacheId The ID of the cache.
 * @param operationStatus The status of the operation.
 */
public record CreateCacheResponse(String cacheId, com.bhf.aeroncache.messages.CacheOperationStatus operationStatus) {

}
