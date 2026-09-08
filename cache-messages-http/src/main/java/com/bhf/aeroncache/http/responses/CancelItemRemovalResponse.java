package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from cancelling a scheduled removal of an item in the cache.
 *
 * @param cacheId         The cache the item's removal was cancelled in.
 * @param key             The key of the item whose removal was cancelled.
 * @param operationStatus The status of the operation.
 */
public record CancelItemRemovalResponse(String cacheId, String key, CacheOperationStatus operationStatus) {
}
