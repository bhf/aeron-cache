package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import com.bhf.aeroncache.types.ReusableString;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class CacheResponseCallbackHandler implements CacheResponseHandler {
    private final CacheResponseHandler observerGroup;

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult) {
        observerGroup.handleCacheEntryResult(getCacheEntryResult);
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult) {
        observerGroup.handleAllCacheEntries(getCacheEntriesResult);
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<ReusableString> createCacheResult) {
        observerGroup.handleCacheCreated(createCacheResult);
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult) {
        observerGroup.handleCacheEntryCreated(addCacheEntryResult);

    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult) {
        observerGroup.handleCacheEntryRemoved(removeCacheEntryResult);
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableString> clearCacheResult) {
        observerGroup.handleCacheCleared(clearCacheResult);
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableString> deleteCacheResult) {
        observerGroup.handleCacheDeleted(deleteCacheResult);
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<ReusableString> statsResult) {
        observerGroup.handleAllCacheStats(statsResult);
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableString> cacheSubscriptionResult) {
        observerGroup.handleCacheSubscribeResponse(cacheSubscriptionResult);
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult) {
        observerGroup.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableString, ReusableString, ReusableString> cacheEntryUpdateResult) {
        observerGroup.handleCacheEntryUpdated(cacheEntryUpdateResult);
    }
}