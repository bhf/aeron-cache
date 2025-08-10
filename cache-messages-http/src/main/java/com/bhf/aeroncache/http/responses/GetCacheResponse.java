package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.messages.OperationStatus;

import java.util.List;

/**
 * The response from getting the entire content of a cache.
 * @param cacheId The cache ID.
 * @param operationStatus The status of the operation.
 * @param items The items in the cache.
 */
public record GetCacheResponse(String cacheId, OperationStatus operationStatus, List<CacheItem> items) {
}
