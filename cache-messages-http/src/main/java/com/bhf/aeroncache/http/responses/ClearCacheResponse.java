package com.bhf.aeroncache.http.responses;

public record ClearCacheResponse(String cacheId, com.bhf.aeroncache.messages.CacheOperationStatus operationStatus) {
}
