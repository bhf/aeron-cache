package com.bhf.aeroncache.http.responses;

import com.bhf.aeroncache.messages.OperationStatus;

public record ClearCacheResponse(String cacheId, OperationStatus operationStatus) {
}
