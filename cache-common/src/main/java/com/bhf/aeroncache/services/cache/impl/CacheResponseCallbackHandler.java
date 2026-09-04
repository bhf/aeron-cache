package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class CacheResponseCallbackHandler<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheResponseHandler<I,K,V> {
    private final CacheResponseHandler<I,K,V> observerGroup;

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<I, K, V> getCacheEntryResult) {
        observerGroup.handleCacheEntryResult(getCacheEntryResult);
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult) {
        observerGroup.handleAllCacheEntries(getCacheEntriesResult);
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<I> createCacheResult) {
        observerGroup.handleCacheCreated(createCacheResult);
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<I, K> addCacheEntryResult) {
        observerGroup.handleCacheEntryCreated(addCacheEntryResult);
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
        observerGroup.handleCacheEntryRemoved(removeCacheEntryResult);
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
        observerGroup.handleCacheCleared(clearCacheResult);
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
        observerGroup.handleCacheDeleted(deleteCacheResult);
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<I> statsResult) {
        observerGroup.handleAllCacheStats(statsResult);
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<I,K,V> cacheSubscriptionResult) {
        observerGroup.handleCacheSubscribeResponse(cacheSubscriptionResult);
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<I> cacheUnsubscribeResult) {
        observerGroup.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {
        observerGroup.handleCacheEntryUpdated(cacheEntryUpdateResult);
    }

    @Override
    public void handleBulkOperationsResult(BulkCacheOpsResult<I, K, V> bulkCacheOpsResult) {
        observerGroup.handleBulkOperationsResult(bulkCacheOpsResult);
    }

    @Override
    public void handleCounterIncremented(IncrementCounterResult<I, K> result) {
        observerGroup.handleCounterIncremented(result);
    }
}