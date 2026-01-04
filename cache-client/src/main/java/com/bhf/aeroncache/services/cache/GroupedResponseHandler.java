package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GroupedResponseHandler implements CacheResponseHandler{

    private final List<CacheResponseHandler> handlers;

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryResult(getCacheEntryResult);
        }
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleAllCacheEntries(getCacheEntriesResult);
        }
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<ReusableString> createCacheResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheCreated(createCacheResult);
        }
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryCreated(addCacheEntryResult);
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryRemoved(removeCacheEntryResult);
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableString> clearCacheResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheCleared(clearCacheResult);
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableString> deleteCacheResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheDeleted(deleteCacheResult);
        }
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<ReusableString> statsResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleAllCacheStats(statsResult);
        }
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableString> cacheSubscriptionResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheSubscribeResponse(cacheSubscriptionResult);
        }
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableString, ReusableString, ReusableString> cacheEntryUpdateResult) {
        for(CacheResponseHandler handler : handlers){
            handler.handleCacheEntryUpdated(cacheEntryUpdateResult);
        }
    }
}
