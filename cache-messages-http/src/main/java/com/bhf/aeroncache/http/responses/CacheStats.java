package com.bhf.aeroncache.http.responses;

public record CacheStats(int totalOpsCount, int totalCachesCount, int totalItemsCount, int errorCount) {
}
