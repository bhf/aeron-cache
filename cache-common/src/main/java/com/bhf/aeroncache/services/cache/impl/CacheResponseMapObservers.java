package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.ConsumingResponseHandler;
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
public class CacheResponseMapObservers<I extends Reusable, K extends Reusable, V extends Reusable> implements ConsumingResponseHandler<I,K,V> {

    final Map<String, Consumer<CreateCacheResult<I>>> createCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<AddCacheEntryResult<I, K>>> addCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<GetCacheEntryResult<I, K, V>>> getCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<DeleteCacheResult<I>>> deleteCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<RemoveCacheEntryResult<I, K>>> removeCacheEntryObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<ClearCacheResult<I>>> clearCacheObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<GetAllCacheEntriesResult<I, K, V>>> getCacheEntriesObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheStatsResult<I>>> allCacheStatsObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheSubscriptionResult<I>>> cacheSubscribeObservers = new ConcurrentHashMap<>();
    final Map<String, Consumer<CacheUnsubscribeResult<I>>> cacheUnsubscribeObservers = new ConcurrentHashMap<>();

    Consumer<CreateCacheResult<I>> createCacheConsumer;
    Consumer<AddCacheEntryResult<I, K>> addCacheEntryConsumer;
    Consumer<ClearCacheResult<I>> clearCacheConsumer;
    Consumer<DeleteCacheResult<I>> deleteCacheConsumer;
    Consumer<RemoveCacheEntryResult<I, K>> removeCacheEntryConsumer;
    Consumer<GetCacheEntryResult<I, K, V>> getCacheEntryConsumer;
    Consumer<GetAllCacheEntriesResult<I, K, V>> getCacheEntriesConsumer;

    public CacheResponseMapObservers() {
    }

    @Override
    public void sendCreateCache(String requestId, String cacheId, Consumer<CreateCacheResult<I>> consumer) {
        createCacheObservers.put(requestId, consumer);
    }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, long ttl, Consumer<AddCacheEntryResult<I, K>> c) {
        addCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void getCacheEntry(String requestId, String cacheId, String key, Consumer<GetCacheEntryResult<I, K, V>> c) {
        getCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void deleteCache(String requestId, String cacheId, Consumer<DeleteCacheResult<I>> consumer) {
        deleteCacheObservers.put(requestId, consumer);
    }

    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key, Consumer<RemoveCacheEntryResult<I, K>> c) {
        removeCacheEntryObservers.put(requestId, c);
    }

    @Override
    public void clearCache(String requestId, String cacheId, Consumer<ClearCacheResult<I>> c) {
        clearCacheObservers.put(requestId, c);
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId, Consumer<GetAllCacheEntriesResult<I, K, V>> c) {
        getCacheEntriesObservers.put(requestId, c);
    }

    @Override
    public void getAllCacheStats(String requestId, Consumer<CacheStatsResult<I>> c) {
        allCacheStatsObservers.put(requestId, c);
    }

    @Override
    public void sendCacheSubscribe(String requestId, String cacheId, Consumer<CacheSubscriptionResult<I>> c) {
        cacheSubscribeObservers.put(requestId, c);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId, Consumer<CacheUnsubscribeResult<I>> c) {
        cacheUnsubscribeObservers.put(requestId, c);
    }

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<I, K, V> getCacheEntryResult) {
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
    public void handleAllCacheEntries(GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult) {
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
    public void handleCacheCreated(CreateCacheResult<I> createCacheResult) {
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
    public void handleCacheEntryCreated(AddCacheEntryResult<I, K> addCacheEntryResult) {
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
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
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
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
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
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
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
    public void handleAllCacheStats(CacheStatsResult<I> cacheStatsResult) {
        var targetId = cacheStatsResult.getRequestId();
        var observer = allCacheStatsObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheStatsResult);
        }
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<I> cacheSubscriptionResult) {
        var targetId = cacheSubscriptionResult.getRequestId();
        log.info("Got cache subscribe response on requestId {}", targetId);
        var observer = cacheSubscribeObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheSubscriptionResult);
        }
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<I> cacheUnsubscribeResult) {
        var targetId = cacheUnsubscribeResult.getRequestId();
        log.info("Got cache unsubscribe response on requestId {}", targetId);
        var observer = cacheUnsubscribeObservers.remove(targetId);
        if (observer != null) {
            observer.accept(cacheUnsubscribeResult);
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {

    }
}