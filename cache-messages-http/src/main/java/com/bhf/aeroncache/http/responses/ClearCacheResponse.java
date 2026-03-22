package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

public record ClearCacheResponse(String cacheId, CacheOperationStatus operationStatus) {
}
