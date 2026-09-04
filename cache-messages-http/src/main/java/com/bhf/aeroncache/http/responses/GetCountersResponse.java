package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

import java.util.List;

/**
 * The response from getting all counters in a counter cache.
 *
 * @param cacheId         The cache ID.
 * @param operationStatus The status of the operation.
 * @param items           The counter items.
 */
public record GetCountersResponse(String cacheId, CacheOperationStatus operationStatus, List<CounterItem> items) {
}
