package com.bhf.aeroncache.models.bulk.requests;

public record CacheOperationRequest(BulkOperationType operationType, long ttl, long counterValue, String requestId, String cacheId, String key, String value) {
}
