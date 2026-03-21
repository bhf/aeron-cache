package com.bhf.aeroncache.http.responses;

/**
 * The response from deleting a cache.
 *
 * @param cacheId         The Cache ID.
 * @param operationStatus The status of the operation.
 */
public record DeleteCacheResponse(String cacheId, com.bhf.aeroncache.messages.CacheOperationStatus operationStatus) {

}
