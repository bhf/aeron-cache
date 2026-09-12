package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.bhf.aeroncache.services.patch.ValuePatchProvider;
import io.aeron.ExclusivePublication;
import io.aeron.Publication;
import io.aeron.cluster.service.Cluster;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
public class MapCache<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCache<I, K, V>{

    final Map<K, V> cache;
    private final V emptyValue;
    private final CacheIdSnapshotCodec<I> cacheIdSnapshotCodec;
    private final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
    private final CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec;
    private final ValuePatchProvider<I,K,V> patchProvider;

    public MapCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier,
                    Supplier<Map<K, V>> mapSupplier, CacheIdSnapshotCodec<I> cacheIdSnapshotCodec,
                    CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec, ValuePatchProvider<I, K, V> patchProvider) {
        super(indexSupplier, keySupplier, valueSupplier);
        this.cache = mapSupplier.get();
        this.emptyValue = valueSupplier.get();
        this.cacheIdSnapshotCodec = cacheIdSnapshotCodec;
        this.cacheEntrySnapshotCodec = cacheEntrySnapshotCodec;
        this.patchProvider = patchProvider;
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
        addCacheEntryResult.setStatus(CacheOperationStatus.SUCCESS);
        stats.addedCount++;
        stats.size = cache.size();
        return addCacheEntryResult;
    }

    @Override
    public AddCacheEntryResult<I, K> add(K key, V value, PatchValueResult<I, K, V> mergePatchOut) {
        V previousValue = mergePatchOut != null ? cache.get(key) : null;
        var result = add(key, value);
        if (mergePatchOut != null) {
            produceMergePatch(key, previousValue, value, mergePatchOut);
        }
        return result;
    }

    private void produceMergePatch(K key, V previousValue, V newValue, PatchValueResult<I, K, V> mergePatchOut) {
        patchProvider.produceMergePatch(key, previousValue, newValue, mergePatchOut);
    }

    @Override
    public GetCacheEntryResult<I, K, V> get(K key) {
        getCacheEntryResult.clear();
        getCacheEntryResult.getEntryKey().copyFrom(key);

        if (cache.containsKey(key)) {
            getCacheEntryResult.getEntryValue().copyFrom(cache.get(key));
            getCacheEntryResult.setStatus(CacheOperationStatus.SUCCESS);
        } else {
            getCacheEntryResult.getEntryValue().copyFrom(emptyValue);
            getCacheEntryResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
        }

        return getCacheEntryResult;
    }

    @Override
    public PatchValueResult<I, K, V> patchValue(K key, V patch) {
        patchValueResult.clear();
        patchValueResult.getEntryKey().copyFrom(key);

        if (!cache.containsKey(key)) {
            patchValueResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            return patchValueResult;
        }

        V existingValue = cache.get(key);
        try {
            if (patchProvider.applyPatch(patch, existingValue)) {
                patchValueResult.getEntryValue().copyFrom(existingValue);
                patchValueResult.setStatus(CacheOperationStatus.SUCCESS);
            } else {
                patchValueResult.getEntryValue().copyFrom(existingValue);
                patchValueResult.setStatus(CacheOperationStatus.ERROR);
            }
        } catch (Exception e) {
            log.warn("Failed to patch value for key {}, reason: {}", key, e.getMessage());
            patchValueResult.getEntryValue().copyFrom(existingValue);
            patchValueResult.setStatus(CacheOperationStatus.ERROR);
        }

        return patchValueResult;
    }


    @Override
    public RemoveCacheEntryResult<I, K> remove(K key) {
        removeCacheEntryResult.clear();
        removeCacheEntryResult.getKey().copyFrom(key);
        var removed = cache.remove(key);
        removeCacheEntryResult.setRemoved(removed != null);
        removeCacheEntryResult.setStatus(removed != null ?
                CacheOperationStatus.SUCCESS : CacheOperationStatus.UNKNOWN_KEY);
        stats.removedCount++;
        stats.size = cache.size();
        return removeCacheEntryResult;
    }

    @Override
    public ClearCacheResult<I> clearEntries() {
        clearCacheResult.clear();
        cache.clear();
        clearCacheResult.setStatus(CacheOperationStatus.SUCCESS);
        stats.clearedCount++;
        stats.size = 0;
        return clearCacheResult;
    }

    @Override
    public Map<K, V> getAllEntries() {
        return cache;
    }

    @Override
    public void takeSnapshot(ExclusivePublication snapshotPublication, I cacheId, Cluster cluster) {
        int offset = cacheIdSnapshotCodec.serializeCacheId(cacheId, buffer, 0);
        offset = stats.encode(buffer, offset);
        int length = offset;

        var allEntries = getAllEntries();
        var sortedKeys = getSortedKeys(allEntries);
        int entriesSnapshotted = 0;

        for (var key : sortedKeys) {
            ++entriesSnapshotted;
            var value = allEntries.get(key);
            length = cacheEntrySnapshotCodec.serializeCacheEntry(key, value, buffer, length);

            if(entriesSnapshotted % 100 == 0) {
                cluster.idleStrategy().idle();
            }
        }

        log.info("Total entries snapshotted in cache {} is {}", cacheId, entriesSnapshotted);

        var result = snapshotPublication.offer(buffer, 0, length);

        if (result < 0) {
            var errorString = Publication.errorString(result);
            log.warn("Failed to snapshot cache {}, reason: {}", cacheId.value(), errorString);
        }
    }

    private List<K> getSortedKeys(Map<K, V> allEntries) {
        List<K> res = new ArrayList<>(allEntries.keySet());
        Comparator<K> keyComparator = cacheEntrySnapshotCodec.getKeyComparator();
        res.sort(keyComparator);
        return res;
    }

    @Override
    public void loadSnapshot(DirectBuffer buffer, int offset) {

        offset = stats.decode(buffer, offset);

        log.info("Total entries to load: {} for cache Id: {}, added: {}", stats.size, stats.getCacheId(), stats.addedCount);
        int added = 0;
        while (added < stats.size) {
            offset = cacheEntrySnapshotCodec.deserializeCacheEntry(buffer, offset, cache);
            added++;
        }

        log.info("Total loaded from snapshot: {}", added);
    }

}
