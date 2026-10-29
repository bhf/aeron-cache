package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from stopping near caching for a cache: the cluster subscription is dropped and the
 * local copy removed.
 *
 * @param cacheId         The ID of the cache.
 * @param wasCached       Whether a local near cache copy existed and was removed.
 * @param operationStatus The status of the operation.
 */
public record StopNearCacheResponse(String cacheId, boolean wasCached, CacheOperationStatus operationStatus) {
}
