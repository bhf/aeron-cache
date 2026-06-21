package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CounterOperationResult;
import com.bhf.aeroncache.services.cache.Cache;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import lombok.extern.log4j.Log4j2;
import org.agrona.collections.Object2ObjectHashMap;

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

    private final Map<I, Cache<I, K, ReusableLong>> counterCaches = new Object2ObjectHashMap<>();
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
        operationResult.getKey().copyFrom(cacheId);

        var cache = counterCaches.get(cacheId);
        if (cache != null) {
            var value = cache.get(key);
            if (value != null) {
                long latestValue = value.getEntryValue().increment(amount);
                operationResult.setCounterValue(latestValue);
                operationResult.setStatus(CacheOperationStatus.SUCCESS);
            } else {
                operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            }
        } else {
            operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
        }

        return operationResult;
    }

    @Override
    public CounterOperationResult<I,K> decrementCounter(I cacheId, K key, long amount) {
        operationResult.clear();
        operationResult.getCacheId().copyFrom(cacheId);
        operationResult.getKey().copyFrom(cacheId);

        var cache = counterCaches.get(cacheId);
        if (cache != null) {
            var value = cache.get(key);
            if (value != null) {
                long latestValue = value.getEntryValue().decrement(amount);
                operationResult.setCounterValue(latestValue);
                operationResult.setStatus(CacheOperationStatus.SUCCESS);
            } else {
                operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            }
        } else {
            operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
        }

        return operationResult;
    }

    @Override
    public CounterOperationResult<I,K> setCounter(I cacheId, K key, long value) {
        operationResult.clear();
        operationResult.getCacheId().copyFrom(cacheId);
        operationResult.getKey().copyFrom(cacheId);

        var cache = counterCaches.get(cacheId);
        if (cache != null) {
            var currentValue = cache.get(key);
            if (currentValue.getStatus() != CacheOperationStatus.UNKNOWN_KEY) {
                currentValue.getEntryValue().copyFrom(value);
                long latestValue = currentValue.getEntryValue().value();
                operationResult.setCounterValue(latestValue);
                operationResult.setStatus(CacheOperationStatus.SUCCESS);
            } else {
                operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            }
        } else {
            operationResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
        }

        return operationResult;
    }
}
