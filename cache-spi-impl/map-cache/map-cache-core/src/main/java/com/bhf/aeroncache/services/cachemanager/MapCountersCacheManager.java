package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CounterOperationResult;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A cache for counters.
 *
 * @param <I> The type of the id of the cache.
 * @param <K> The key type for the caches.
 */
@Log4j2
public class MapCountersCacheManager<I extends Reusable, K extends Reusable> extends MapCacheManager<I, K, ReusableLong> implements CountersCacheManager<I,K,ReusableLong>{

    private final CounterOperationResult<I,K> operationResult;

    public MapCountersCacheManager(Supplier<I> cacheIndexSupplier, Supplier<K> cacheKeySupplier,
                                   Supplier<ReusableLong> cacheValueSupplier, Supplier<Map<K, ReusableLong>> mapSupplier,
                                   CacheIdSnapshotCodec<I> cacheIdSnapshotCodec,
                                   CacheEntrySnapshotCodec<K, ReusableLong> cacheEntrySnapshotCodec) {
        super(cacheIndexSupplier, cacheKeySupplier, cacheValueSupplier, mapSupplier, cacheIdSnapshotCodec, cacheEntrySnapshotCodec);
        operationResult = new CounterOperationResult<>(cacheIndexSupplier.get(), cacheKeySupplier.get());
    }


    @Override
    public CounterOperationResult<I,K> incrementCounter(I cacheId, K key, long amount) {
        operationResult.clear();
        operationResult.getCacheId().copyFrom(cacheId);
        operationResult.getKey().copyFrom(key);

        var cache = getCache(cacheId);
        if (cache != null) {
            var value = cache.getAllEntries().get(key);
            if (value != null) {
                long latestValue = value.increment(amount);
                operationResult.setCounterValue(latestValue);
                operationResult.setStatus(CacheOperationStatus.SUCCESS);
            } else {
                operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            }
        } else {
            operationResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }

        return operationResult;
    }

    @Override
    public CounterOperationResult<I,K> decrementCounter(I cacheId, K key, long amount) {
        operationResult.clear();
        operationResult.getCacheId().copyFrom(cacheId);
        operationResult.getKey().copyFrom(key);

        var cache = getCache(cacheId);
        if (cache != null) {
            var value = cache.getAllEntries().get(key);
            if (value != null) {
                long latestValue = value.decrement(amount);
                operationResult.setCounterValue(latestValue);
                operationResult.setStatus(CacheOperationStatus.SUCCESS);
            } else {
                operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            }
        } else {
            operationResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }

        return operationResult;
    }

    @Override
    public CounterOperationResult<I,K> setCounter(I cacheId, K key, long value) {
        operationResult.clear();
        operationResult.getCacheId().copyFrom(cacheId);
        operationResult.getKey().copyFrom(key);

        var cache = getCache(cacheId);
        if (cache != null) {
            var storedValue = cache.getAllEntries().get(key);
            if (storedValue != null) {
                storedValue.copyFrom(value);
                operationResult.setCounterValue(storedValue.value());
                operationResult.setStatus(CacheOperationStatus.SUCCESS);
            } else {
                operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            }
        } else {
            operationResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }

        return operationResult;
    }
}
