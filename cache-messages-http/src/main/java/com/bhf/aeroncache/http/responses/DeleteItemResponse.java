package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.messages.OperationStatus;

/**
 * The response from deleting an item from the cache.
 *
 * @param cacheId         The cache the item was removed from.
 * @param key             The key of the item removed.
 * @param operationStatus The status of the operation.
 */
public record DeleteItemResponse(String cacheId, String key, OperationStatus operationStatus) {
}
