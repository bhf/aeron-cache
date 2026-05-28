package com.bhf.aeroncache.models.bulk.responses;

import java.util.List;

public record BulkCacheOpsResponse(String requestId, List<CacheOperationResponse> operationResponses) {
}
