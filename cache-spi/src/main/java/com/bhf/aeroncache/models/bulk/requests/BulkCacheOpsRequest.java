package com.bhf.aeroncache.models.bulk.requests;

import java.util.List;

public record BulkCacheOpsRequest(String requestId, List<? extends CacheOperation> operations){

}
