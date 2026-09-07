package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GroupedResponseHandler<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheResponseHandler<I, K, V> {

    private final List<CacheResponseHandler> handlers;

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<I, K, V> getCacheEntryResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryResult(getCacheEntryResult);
        }
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleAllCacheEntries(getCacheEntriesResult);
        }
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<I> createCacheResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheCreated(createCacheResult);
        }
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<I, K> addCacheEntryResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryCreated(addCacheEntryResult);
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryRemoved(removeCacheEntryResult);
        }
    }

    @Override
    public void handleCacheEntryPatched(PatchValueResult<I, K, V> patchValueResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryPatched(patchValueResult);
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheCleared(clearCacheResult);
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheDeleted(deleteCacheResult);
        }
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<I> statsResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleAllCacheStats(statsResult);
        }
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<I,K,V> cacheSubscriptionResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheSubscribeResponse(cacheSubscriptionResult);
        }
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<I> cacheUnsubscribeResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryUpdated(cacheEntryUpdateResult);
        }
    }

    @Override
    public void handleBulkOperationsResult(BulkCacheOpsResult<I, K, V> bulkCacheOpsResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleBulkOperationsResult(bulkCacheOpsResult);
        }
    }
}
