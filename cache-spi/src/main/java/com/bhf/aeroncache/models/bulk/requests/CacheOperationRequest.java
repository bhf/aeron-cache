package com.bhf.aeroncache.models.bulk.requests;

public record CacheOperationRequest(BulkOperationType operationType, long ttl, String requestId, String cacheId, String key, String value) {
}
