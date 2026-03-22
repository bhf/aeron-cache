package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.ConsumingResponseHandler;
import com.bhf.aeroncache.types.ReusableString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Use multiple {@link ConcurrentHashMap} instances to store {@link Consumer} callbacks.
 * See the JMH test: CacheResponseMapObserversJMH vs CacheResponseObserversJMH - illustrates
 * that copy on write activity becomes dominant under heavy write loads due to the nature of
 * one shot request-response vs. "initialized once long-lived observers".
 */
@Getter
@Setter
@Log4j2
public class CacheResponseMapObservers implements ConsumingResponseHandler {

    final Map<String, Consumer<CreateCacheResult<ReusableString>>> createCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<AddCacheEntryResult<ReusableString, ReusableString>>> addCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<GetCacheEntryResult<ReusableString, ReusableString, ReusableString>>> getCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<DeleteCacheResult<ReusableString>>> deleteCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>>> removeCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<ClearCacheResult<ReusableString>>> clearCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>>> getCacheEntriesObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheStatsResult<ReusableString>>> allCacheStatsObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheSubscriptionResult<ReusableString>>> cacheSubscribeObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheUnsubscribeResult<ReusableString>>> cacheUnsubscribeObservers = new ConcurrentHashMap<>();

    Consumer<CreateCacheResult<ReusableString>> createCacheConsumer;
    Consumer<AddCacheEntryResult<ReusableString, ReusableString>> addCacheEntryConsumer;
    Consumer<ClearCacheResult<ReusableString>> clearCacheConsumer;
    Consumer<DeleteCacheResult<ReusableString>> deleteCacheConsumer;
    Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> removeCacheEntryConsumer;
    Consumer<GetCacheEntryResult<ReusableString, ReusableString, ReusableString>> getCacheEntryConsumer;
    Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>> getCacheEntriesConsumer;

    public CacheResponseMapObservers() {
    }

    @Override
    public void sendCreateCache(String requestId, String cacheId, Consumer<CreateCacheResult<ReusableString>> consumer) {
        createCacheObservers.put(requestId, consumer);
    }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableString, ReusableString>> c) {
        addCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void getCacheEntry(String requestId, String cacheId, String key, Consumer<GetCacheEntryResult<ReusableString, ReusableString, ReusableString>> c) {
        getCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void deleteCache(String requestId, String cacheId, Consumer<DeleteCacheResult<ReusableString>> consumer) {
        deleteCacheObservers.put(requestId, consumer);
    }

    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> c) {
        removeCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void clearCache(String requestId, String cacheId, Consumer<ClearCacheResult<ReusableString>> c) {
        clearCacheObservers.put(requestId, c);
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId, Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>> c) {
        getCacheEntriesObservers.put(requestId, c);
    }

    @Override
    public void getAllCacheStats(String requestId, Consumer<CacheStatsResult<ReusableString>> c) {
        allCacheStatsObservers.put(requestId, c);
    }

    @Override
    public void sendCacheSubscribe(String requestId, String cacheId, Consumer<CacheSubscriptionResult<ReusableString>> c) {
        cacheSubscribeObservers.put(requestId, c);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId, Consumer<CacheUnsubscribeResult<ReusableString>> c) {
        cacheUnsubscribeObservers.put(requestId, c);
    }

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult) {
        var targetId = getCacheEntryResult.getRequestId();
        var observer = getCacheEntryObservers.remove(targetId);
        if (observer != null) {
            observer.accept(getCacheEntryResult);
        }

        if (getCacheEntryConsumer != null) {
            getCacheEntryConsumer.accept(getCacheEntryResult);
        }
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult) {
        var targetId = getCacheEntriesResult.getRequestId();
        var observer = getCacheEntriesObservers.remove(targetId);
        if (observer != null) {
            observer.accept(getCacheEntriesResult);
        }

        if (getCacheEntriesConsumer != null) {
            getCacheEntriesConsumer.accept(getCacheEntriesResult);
        }
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<ReusableString> createCacheResult) {
        var targetId = createCacheResult.getRequestId();
        var observer = createCacheObservers.remove(targetId);
        if (observer != null) {
            observer.accept(createCacheResult);
        }

        if (createCacheConsumer != null) {
            createCacheConsumer.accept(createCacheResult);
        }
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult) {
        var targetId = addCacheEntryResult.getRequestId();
        var observer = addCacheEntryObservers.remove(targetId);
        if (observer != null) {
            observer.accept(addCacheEntryResult);
        }

        if (addCacheEntryConsumer != null) {
            addCacheEntryConsumer.accept(addCacheEntryResult);
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult) {
        var targetId = removeCacheEntryResult.getRequestId();
        var observer = removeCacheEntryObservers.remove(targetId);
        if (observer != null) {
            observer.accept(removeCacheEntryResult);
        }

        if (removeCacheEntryConsumer != null) {
            removeCacheEntryConsumer.accept(removeCacheEntryResult);
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableString> clearCacheResult) {
        var targetId = clearCacheResult.getRequestId();
        var observer = clearCacheObservers.remove(targetId);
        if (observer != null) {
            observer.accept(clearCacheResult);
        }

        if (clearCacheConsumer != null) {
            clearCacheConsumer.accept(clearCacheResult);
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableString> deleteCacheResult) {
        var targetId = deleteCacheResult.getRequestId();
        var observer = deleteCacheObservers.remove(targetId);
        if (observer != null) {
            observer.accept(deleteCacheResult);
        }

        if (deleteCacheConsumer != null) {
            deleteCacheConsumer.accept(deleteCacheResult);
        }
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<ReusableString> cacheStatsResult) {
        var targetId = cacheStatsResult.getRequestId();
        var observer = allCacheStatsObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheStatsResult);
        }
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableString> cacheSubscriptionResult) {
        var targetId = cacheSubscriptionResult.getRequestId();
        log.info("Got cache subscribe response on requestId {}", targetId);
        var observer = cacheSubscribeObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheSubscriptionResult);
        }
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult) {
        var targetId = cacheUnsubscribeResult.getRequestId();
        log.info("Got cache unsubscribe response on requestId {}", targetId);
        var observer = cacheUnsubscribeObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheUnsubscribeResult);
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableString, ReusableString, ReusableString> cacheEntryUpdateResult) {

    }
}