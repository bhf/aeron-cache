package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.services.cache.CacheEntryCodec;
import com.bhf.aeroncache.services.cache.CacheIdCodec;
import io.aeron.ExclusivePublication;
import io.aeron.Publication;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A cache implementation backed by a Java {@link Map} implementation.
 *
 * @param <I> The type the cache is indexed on.
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
@Log4j2
public class MapCache<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCache<I, K, V> {

    final Map<K, V> cache;
    private final V emptyValue;
    private final CacheIdCodec<I> cacheIdSerializer;
    private final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
    private final CacheEntryCodec<K, V> cacheEntryCodec;

    public MapCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier,
                    Supplier<Map<K, V>> mapSupplier, CacheIdCodec<I> cacheIdSerializer,
                    CacheEntryCodec<K, V> cacheEntrySerializer) {
        super(indexSupplier, keySupplier, valueSupplier);
        this.cache = mapSupplier.get();
        this.emptyValue = valueSupplier.get();
        this.cacheIdSerializer = cacheIdSerializer;
        this.cacheEntryCodec = cacheEntrySerializer;
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
        stats.addedCount++;
        stats.size = cache.size();
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
        stats.removedCount++;
        stats.size = cache.size();
        return removeCacheEntryResult;
    }

    @Override
    public ClearCacheResult<I> clearEntries() {
        clearCacheResult.clear();
        cache.clear();
        clearCacheResult.setStatus(OperationStatus.SUCCESS);
        stats.clearedCount++;
        stats.size = 0;
        return clearCacheResult;
    }

    @Override
    public Map<K, V> getAllEntries() {
        return cache;
    }

    @Override
    public void takeSnapshot(ExclusivePublication snapshotPublication, I cacheId) {
        int offset = cacheIdSerializer.serializeCacheId(cacheId, buffer, 0);
        offset = stats.encode(buffer, offset);
        int length = offset;

        var allEntries = getAllEntries();
        for (var entry : allEntries.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            length = cacheEntryCodec.serialize(key, value, buffer, offset);
        }

        var result = snapshotPublication.offer(buffer, 0, length);

        if (result < 0) {
            var errorString = Publication.errorString(result);
            log.warn("Failed to snapshot cache {}, reason: {}", cacheId.value(), errorString);
        }
    }

    @Override
    public void loadSnapshot(DirectBuffer buffer, int offset) {
        offset = stats.decode(buffer, offset);

        int added = 0;
        while (added < stats.size) {
            offset = cacheEntryCodec.deserialize(buffer, offset, cache);
        }
    }

}
