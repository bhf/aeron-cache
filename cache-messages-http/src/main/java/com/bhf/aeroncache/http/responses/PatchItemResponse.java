package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from patching an item in the cache.
 *
 * @param cacheId         The cache the item was patched in.
 * @param key             The key of the item patched.
 * @param operationStatus The status of the operation.
 */
public record PatchItemResponse(String cacheId, String key, CacheOperationStatus operationStatus) {
}
