package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.http.responses.CacheStats;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class CacheStatsTracker {
    private final AtomicInteger totalErrors = new AtomicInteger();
    private final AtomicInteger totalCaches = new AtomicInteger();
    private final AtomicInteger totalItems = new AtomicInteger();
    private final AtomicInteger totalOpsCount = new AtomicInteger();

    public CacheStats getCacheStats(){
        return new CacheStats(totalOpsCount.get(), totalCaches.get(), totalItems.get(), totalErrors.get());
    }
}
