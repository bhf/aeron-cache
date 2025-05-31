package com.bhf.aeroncache.services.cachemanager.impl;

import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.Cache;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A cache manager which indexes cache instances based on a type I.
 * Uses a Java HashMap.
 *
 * @param <I> The type of the id of the cache.
 * @param <K> The key type for the caches.
 * @param <V> The value type for the caches.
 */
@Log4j2
public abstract class AbstractHashMapCacheManager<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCacheManager<I, K, V> {

    private final HashMap<I, Cache<I, K, V>> caches = new HashMap<>();
    private final Supplier<Map<K,V>> mapSupplier;

    public AbstractHashMapCacheManager(Supplier<I> cacheIndexSupplier, Supplier<K> cacheKeySupplier, Supplier<V> cacheValueSupplier, Supplier<Map<K, V>> mapSupplier) {
        super(cacheIndexSupplier, cacheKeySupplier, cacheValueSupplier);
        this.mapSupplier = mapSupplier;
    }

    @Override
    public Cache<I, K, V> getCache(I cacheId) {
        return caches.get(cacheId);
    }

    @Override
    public ClearCacheResult<I> clearCache(I cacheId) {
        clearCacheResult.clear();
        clearCacheResult.getCacheId().copyFrom(cacheId);
        var cache = getCache(cacheId);
        if (cache != null) {
            cache.clearEntries();
            clearCacheResult.setStatus(OperationStatus.SUCCESS);
        } else {
            clearCacheResult.setStatus(OperationStatus.UNKNOWN_CACHE);
        }
        return clearCacheResult;
    }

    @Override
    public CreateCacheResult<I> createCache(I cacheId) {
        cacheCreationResult.clear();
        cacheCreationResult.getCacheId().copyFrom(cacheId);

        log.info("Known caches {}", caches.keySet());

        if (caches.containsKey(cacheId)) {
            cacheCreationResult.setStatus(OperationStatus.CACHE_EXISTS);
            return cacheCreationResult;
        }

        var cache = cacheFactory.getNewCache(indexSupplier, keySupplier, valueSupplier, mapSupplier);
        I newKey = indexSupplier.get();
        newKey.copyFrom(cacheId);
        caches.put(newKey, cache);
        cacheCreationResult.setStatus(OperationStatus.SUCCESS);
        return cacheCreationResult;
    }

    @Override
    public DeleteCacheResult<I> deleteCache(I cacheId) {
        deleteCacheResult.clear();
        deleteCacheResult.getCacheId().copyFrom(cacheId);
        var removed = caches.remove(cacheId);

        if (removed == null) {
            log.info("Tried to remove unknown cache {}, known caches: {}", cacheId, caches.keySet());
        }

        deleteCacheResult.setStatus(removed != null ?
                OperationStatus.SUCCESS : OperationStatus.UNKNOWN_CACHE);
        return deleteCacheResult;
    }

    @Override
    public RemoveCacheEntryResult<I, K> removeCacheEntry(I cacheId, K key) {
        removeCacheEntryResult.clear();
        removeCacheEntryResult.getCacheId().copyFrom(cacheId);
        var cache = getCache(cacheId);
        if (cache != null) {
            var result = cache.remove(key);
            removeCacheEntryResult.getKey().copyFrom(result.getKey());
            removeCacheEntryResult.setStatus(result.getStatus());
        } else {
            removeCacheEntryResult.setStatus(OperationStatus.UNKNOWN_CACHE);
        }
        return removeCacheEntryResult;
    }

    @Override
    public GetCacheEntryResult<I, K, V> getCacheEntry(I cacheId, K key) {
        getCacheEntryResult.clear();
        getCacheEntryResult.getCacheId().copyFrom(cacheId);
        var cache = getCache(cacheId);
        if (cache != null) {
            var result = cache.get(key);
            getCacheEntryResult.getEntryKey().copyFrom(result.getEntryKey());
            getCacheEntryResult.getEntryValue().copyFrom(result.getEntryValue());
            getCacheEntryResult.setStatus(result.getStatus());
        } else {
            getCacheEntryResult.setStatus(OperationStatus.UNKNOWN_CACHE);
        }
        return getCacheEntryResult;
    }

    @Override
    public GetAllCacheEntriesResult<I, K, V> getAllCacheEntries(I cacheId) {
        getAllCacheEntriesResult.clear();
        getAllCacheEntriesResult.getCacheId().copyFrom(cacheId);
        var cache = getCache(cacheId);
        if (cache != null) {
            getAllCacheEntriesResult.setStatus(OperationStatus.SUCCESS);
            getAllCacheEntriesResult.getValues().putAll(cache.getAllEntries());
        } else {
            getAllCacheEntriesResult.setStatus(OperationStatus.UNKNOWN_CACHE);
        }
        return getAllCacheEntriesResult;
    }

    @Override
    public CacheStatsResult getCacheStatsResult() {
        allCacheStatsResult.clear();

        caches.forEach((cacheId, cache) -> {
            var stats = cache.getCacheStats();
            log.info("Got cache stats on cacheId {}", cacheId);
            stats.getCacheId().copyFrom(cacheId);
            allCacheStatsResult.getStats().add(stats);
        });

        return allCacheStatsResult;
    }
}
