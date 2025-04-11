package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.messages.OperationStatus;

/**
 * The response from getting an item from the cache.
 *
 * @param cacheId         The ID of the cache.
 * @param key             The key of the item.
 * @param value           The value of the item.
 * @param operationStatus The status of the operation.
 */
public record GetItemResponse(long cacheId, String key, String value, OperationStatus operationStatus) {
}
