package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.messages.OperationStatus;

/**
 * The response from deleting a cache.
 *
 * @param cacheId         The Cache ID.
 * @param operationStatus The status of the operation.
 */
public record DeleteCacheResponse(String cacheId, OperationStatus operationStatus) {

}
