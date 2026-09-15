package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

import java.util.List;

/**
 * The response from getting all pending TTL removal timers across caches and counter caches.
 *
 * @param operationStatus The status of the operation.
 * @param timers          The pending timers, each tagged with its type (cache or counter).
 */
public record GetTimersResponse(CacheOperationStatus operationStatus, List<TimerInfo> timers) {
}
