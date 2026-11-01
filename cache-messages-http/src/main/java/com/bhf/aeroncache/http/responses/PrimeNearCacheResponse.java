package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

/**
 * The response from priming a near cache for a cache: the internal near cache subscription is started
 * with a snapshot so the local copy is warmed up front, before any hydrating client subscription
 * arrives.
 *
 * <p>The snapshot that populates the local copy arrives asynchronously over the cluster subscription,
 * so a {@code SUCCESS} response means priming was started, not that the local copy is fully populated
 * yet. The current item count can be read from the warm caches listing shortly afterwards.
 *
 * @param cacheId         The ID of the cache.
 * @param wasAlreadyWarm  Whether the cache was already warm, in which case priming was a no-op.
 * @param operationStatus The status of the operation.
 */
public record PrimeNearCacheResponse(String cacheId, boolean wasAlreadyWarm, CacheOperationStatus operationStatus) {
}
