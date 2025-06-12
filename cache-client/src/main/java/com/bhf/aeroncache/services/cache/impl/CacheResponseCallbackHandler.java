package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class CacheResponseCallbackHandler implements CacheResponseHandler {
    private final CacheResponseObservers observerGroup;

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult) {
        var targetId = getCacheEntryResult.getRequestId();
        observerGroup.getCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntryResult));
        observerGroup.getCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (observerGroup.getCacheEntryConsumer != null) {
            observerGroup.getCacheEntryConsumer.accept(getCacheEntryResult);
        }
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult) {
        var targetId = getCacheEntriesResult.getRequestId();
        observerGroup.getCacheEntriesObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntriesResult));
        observerGroup.getCacheEntriesObservers.removeIf(p -> p.getId().equals(targetId));
        if (observerGroup.getCacheEntriesConsumer != null) {
            observerGroup.getCacheEntriesConsumer.accept(getCacheEntriesResult);
        }
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<ReusableLong> createCacheResult) {
        var targetId = createCacheResult.getRequestId();
        observerGroup.createCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(createCacheResult));
        observerGroup.createCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (observerGroup.createCacheConsumer != null) {
            observerGroup.createCacheConsumer.accept(createCacheResult);
        }
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult) {
        var targetId = addCacheEntryResult.getRequestId();
        observerGroup.addCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(addCacheEntryResult));
        observerGroup.addCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (observerGroup.addCacheEntryConsumer != null) {
            observerGroup.addCacheEntryConsumer.accept(addCacheEntryResult);
        }

    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
        var targetId = removeCacheEntryResult.getRequestId();
        observerGroup.removeCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(removeCacheEntryResult));
        observerGroup.removeCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (observerGroup.removeCacheEntryConsumer != null) {
            observerGroup.removeCacheEntryConsumer.accept(removeCacheEntryResult);
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableLong> clearCacheResult) {
        var targetId = clearCacheResult.getRequestId();
        observerGroup.clearCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(clearCacheResult));
        observerGroup.clearCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (observerGroup.clearCacheConsumer != null) {
            observerGroup.clearCacheConsumer.accept(clearCacheResult);
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableLong> deleteCacheResult) {
        var targetId = deleteCacheResult.getRequestId();
        observerGroup.deleteCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(deleteCacheResult));
        observerGroup.deleteCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (observerGroup.deleteCacheConsumer != null) {
            observerGroup.deleteCacheConsumer.accept(deleteCacheResult);
        }
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<ReusableLong> statsResult) {
        var targetId = statsResult.getRequestId();
        observerGroup.allCacheStatsObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(statsResult));
        observerGroup.allCacheStatsObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult) {
        var targetId = cacheSubscriptionResult.getRequestId();
        log.info("Got cache subscribe response on requestId {}", targetId);
        observerGroup.cacheSubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheSubscriptionResult));
        observerGroup.cacheSubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult) {
        var targetId = cacheUnsubscribeResult.getRequestId();
        log.info("Got cache unsubscribe response on requestId {}", targetId);
        observerGroup.cacheUnsubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheUnsubscribeResult));
        observerGroup.cacheUnsubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult) {

    }
}