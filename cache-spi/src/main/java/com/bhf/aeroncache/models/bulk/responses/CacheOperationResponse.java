package com.bhf.aeroncache.models.bulk.responses;

import com.bhf.aeroncache.models.results.CacheOperationStatus;

public record CacheOperationResponse(String requestId, CacheOperationStatus status, String cacheId, String key, String value) {
}