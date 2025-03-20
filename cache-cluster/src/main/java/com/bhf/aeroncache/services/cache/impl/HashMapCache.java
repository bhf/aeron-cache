package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A cache implementation backed by an on heap {@link HashMap}.
 *
 * @param <I> The type the cache is indexed on.
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
@Log4j2
public class HashMapCache<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCache<I, K, V> {

    final Map<K, V> cache = new HashMap<>();
    private final V emptyValue;

    public HashMapCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier) {
        super(indexSupplier, keySupplier, valueSupplier);
        this.emptyValue = valueSupplier.get();
    }

    @Override
    public AddCacheEntryResult<I, K> add(K key, V value) {
        addCacheEntryResult.clear();
        addCacheEntryResult.getEntryKey().copyFrom(key);
        K newKey = keySupplier.get();
        newKey.copyFrom(key);
        V newValue = valueSupplier.get();
        newValue.copyFrom(value);
        cache.put(newKey, newValue);
        addCacheEntryResult.setEntryAdded(true);
        addCacheEntryResult.setStatus(OperationStatus.SUCCESS);
        return addCacheEntryResult;
    }

    @Override
    public GetCacheEntryResult<I, K, V> get(K key) {
        getCacheEntryResult.clear();
        getCacheEntryResult.getEntryKey().copyFrom(key);

        if (cache.containsKey(key)) {
            getCacheEntryResult.getEntryValue().copyFrom(cache.get(key));
            getCacheEntryResult.setStatus(OperationStatus.SUCCESS);
        } else {
            getCacheEntryResult.getEntryValue().copyFrom(emptyValue);
            getCacheEntryResult.setStatus(OperationStatus.UNKNOWN_KEY);
        }

        return getCacheEntryResult;
    }

    @Override
    public RemoveCacheEntryResult<I, K> remove(K key) {
        removeCacheEntryResult.clear();
        removeCacheEntryResult.getKey().copyFrom(key);
        var removed = cache.remove(key);
        removeCacheEntryResult.setRemoved(removed != null);
        removeCacheEntryResult.setStatus(removed != null ?
                OperationStatus.SUCCESS : OperationStatus.UNKNOWN_KEY);
        return removeCacheEntryResult;
    }

    @Override
    public ClearCacheResult<I> clearEntries() {
        clearCacheResult.clear();
        cache.clear();
        clearCacheResult.setStatus(OperationStatus.SUCCESS);
        return clearCacheResult;
    }
}
