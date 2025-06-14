package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.ConsumingResponseHandler;
import com.bhf.aeroncache.types.ReusableLong;
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

    final Map<String, Consumer<CreateCacheResult<ReusableLong>>> createCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>>> addCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>>> getCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<DeleteCacheResult<ReusableLong>>> deleteCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>>> removeCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<ClearCacheResult<ReusableLong>>> clearCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>>> getCacheEntriesObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheStatsResult<ReusableLong>>> allCacheStatsObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheSubscriptionResult<ReusableLong>>> cacheSubscribeObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheUnsubscribeResult<ReusableLong>>> cacheUnsubscribeObservers = new ConcurrentHashMap<>();

    Consumer<CreateCacheResult<ReusableLong>> createCacheConsumer;
    Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> addCacheEntryConsumer;
    Consumer<ClearCacheResult<ReusableLong>> clearCacheConsumer;
    Consumer<DeleteCacheResult<ReusableLong>> deleteCacheConsumer;
    Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> removeCacheEntryConsumer;
    Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> getCacheEntryConsumer;
    Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> getCacheEntriesConsumer;

    public CacheResponseMapObservers() {
    }

    @Override
    public void sendCreateCache(String requestId, long cacheId, Consumer<CreateCacheResult<ReusableLong>> consumer) {
        createCacheObservers.put(requestId, consumer);
    }

    @Override
    public void addCacheEntry(String requestId, long cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c) {
        addCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void getCacheEntry(String requestId, long cacheId, String key, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c) {
        getCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void deleteCache(String requestId, long cacheId, Consumer<DeleteCacheResult<ReusableLong>> consumer) {
        deleteCacheObservers.put(requestId, consumer);
    }

    @Override
    public void removeCacheEntry(String requestId, long cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c) {
        removeCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void clearCache(String requestId, long cacheId, Consumer<ClearCacheResult<ReusableLong>> c) {
        clearCacheObservers.put(requestId, c);
    }

    @Override
    public void getCacheEntries(String requestId, long cacheId, Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> c) {
        getCacheEntriesObservers.put(requestId, c);
    }

    @Override
    public void getAllCacheStats(String requestId, Consumer<CacheStatsResult<ReusableLong>> c) {
        allCacheStatsObservers.put(requestId, c);
    }

    @Override
    public void sendCacheSubscribe(String requestId, long cacheId, Consumer<CacheSubscriptionResult<ReusableLong>> c) {
        cacheSubscribeObservers.put(requestId, c);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, long cacheId, Consumer<CacheUnsubscribeResult<ReusableLong>> c) {
        cacheUnsubscribeObservers.put(requestId, c);
    }

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult) {
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
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult) {
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
    public void handleCacheCreated(CreateCacheResult<ReusableLong> createCacheResult) {
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
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult) {
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
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
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
    public void handleCacheCleared(ClearCacheResult<ReusableLong> clearCacheResult) {
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
    public void handleCacheDeleted(DeleteCacheResult<ReusableLong> deleteCacheResult) {
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
    public void handleAllCacheStats(CacheStatsResult<ReusableLong> cacheStatsResult) {
        var targetId = cacheStatsResult.getRequestId();
        var observer = allCacheStatsObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheStatsResult);
        }
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult) {
        var targetId = cacheSubscriptionResult.getRequestId();
        log.info("Got cache subscribe response on requestId {}", targetId);
        var observer = cacheSubscribeObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheSubscriptionResult);
        }
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult) {
        var targetId = cacheUnsubscribeResult.getRequestId();
        log.info("Got cache unsubscribe response on requestId {}", targetId);
        var observer = cacheUnsubscribeObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheUnsubscribeResult);
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult) {

    }
}