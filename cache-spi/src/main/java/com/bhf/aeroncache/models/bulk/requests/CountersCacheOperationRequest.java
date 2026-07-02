package com.bhf.aeroncache.models.bulk.requests;

public record CountersCacheOperationRequest(CountersBulkOperationType operationType, long ttl, String requestId, String cacheId, String key, long value) {
}
