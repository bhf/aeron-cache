package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.Cache;
import com.bhf.aeroncache.services.cache.MapCacheFactory;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.SnapshotRecords;
import io.aeron.ExclusivePublication;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.FragmentHandler;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.MutableBoolean;
import org.agrona.collections.Object2ObjectHashMap;

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
public class MapCacheManager<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCacheManager<I, K, V>{

    private final Map<I, Cache<I, K, V>> caches = new Object2ObjectHashMap<>();
    private final Supplier<Map<K, V>> mapSupplier;
    private final CacheIdSnapshotCodec<I> cacheIdSnapshotCodec;
    private final CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec;
    private final MapCacheFactory<I, K, V> cacheFactory = new MapCacheFactory<>();
    private final MutableDirectBuffer endMarkerBuffer = new ExpandableArrayBuffer(SnapshotRecords.TYPE_LENGTH);

    public MapCacheManager(Supplier<I> cacheIndexSupplier, Supplier<K> cacheKeySupplier,
                           Supplier<V> cacheValueSupplier, Supplier<Map<K, V>> mapSupplier,
                           CacheIdSnapshotCodec<I> cacheIdSnapshotCodec,
                           CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec) {
        super(cacheIndexSupplier, cacheKeySupplier, cacheValueSupplier);
        this.mapSupplier = mapSupplier;
        this.cacheIdSnapshotCodec = cacheIdSnapshotCodec;
        this.cacheEntrySnapshotCodec = cacheEntrySnapshotCodec;
    }

    @Override
    public void takeSnapshot(ExclusivePublication snapshotPublication, Cluster cluster) {
        int totalCachesSnapshotted = 0;
        for (var cacheEntry : caches.entrySet()) {
            ++totalCachesSnapshotted;
            var cacheId = cacheEntry.getKey();
            var cache = cacheEntry.getValue();
            cache.takeSnapshot(snapshotPublication, cacheId, cluster);
        }

        // Mark the end of this manager's records
        offerManagerEnd(snapshotPublication, cluster);

        log.info("Total caches snapshotted: {}", totalCachesSnapshotted);
    }

    private void offerManagerEnd(ExclusivePublication snapshotPublication, Cluster cluster) {
        endMarkerBuffer.putInt(0, SnapshotRecords.MANAGER_END);
        while (snapshotPublication.offer(endMarkerBuffer, 0, SnapshotRecords.TYPE_LENGTH) < 0) {
            cluster.idleStrategy().idle();
        }
    }

    @Override
    public void loadSnapshot(Image snapshotImage) {
        MutableBoolean finished = new MutableBoolean(false);
        I currentCacheId = indexSupplier.get();
        // One-element holder so the lambda can track the cache named by the most recent CACHE_BEGIN.
        @SuppressWarnings("unchecked")
        Cache<I, K, V>[] currentCache = new Cache[1];

        FragmentHandler handler = (buffer, offset, length, header) -> {
            int type = buffer.getInt(offset);
            int recordOffset = offset + SnapshotRecords.TYPE_LENGTH;

            switch (type) {
                case SnapshotRecords.CACHE_BEGIN -> {
                    int statsOffset = cacheIdSnapshotCodec.deserializeCacheId(buffer, recordOffset, currentCacheId);
                    var createResult = createCache(currentCacheId);
                    if (createResult.getStatus() == CacheOperationStatus.SUCCESS) {
                        var cache = getCache(currentCacheId);
                        cache.applyStats(buffer, statsOffset);
                        cache.getCacheStats().getCacheId().copyFrom(currentCacheId);
                        currentCache[0] = cache;
                        log.info("Loading snapshot for cache Id: {}", currentCacheId);
                    } else {
                        currentCache[0] = null;
                        log.warn("Couldn't create cache on cache Id {}, status: {}", currentCacheId.value(),
                                createResult.getStatus());
                    }
                }
                case SnapshotRecords.CACHE_ENTRY -> {
                    if (currentCache[0] != null) {
                        currentCache[0].loadEntry(buffer, recordOffset);
                    }
                }
                case SnapshotRecords.MANAGER_END -> finished.set(true);
                default -> log.warn("Unknown snapshot record type {} while loading cache manager snapshot", type);
            }
        };

        var assembler = new FragmentAssembler(handler);

        while (!finished.get() && !snapshotImage.isEndOfStream()) {
            snapshotImage.poll(assembler, 1);
        }
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
            clearCacheResult.setStatus(CacheOperationStatus.SUCCESS);
        } else {
            clearCacheResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }
        return clearCacheResult;
    }

    @Override
    public CreateCacheResult<I> createCache(I cacheId) {
        cacheCreationResult.clear();
        cacheCreationResult.getCacheId().copyFrom(cacheId);

        if (caches.containsKey(cacheId)) {
            cacheCreationResult.setStatus(CacheOperationStatus.CACHE_EXISTS);
            return cacheCreationResult;
        }

        var cache = cacheFactory.getNewCache(indexSupplier, keySupplier, valueSupplier, mapSupplier, cacheIdSnapshotCodec, cacheEntrySnapshotCodec);
        I newKey = indexSupplier.get();
        newKey.copyFrom(cacheId);
        caches.put(newKey, cache);
        cacheCreationResult.setStatus(CacheOperationStatus.SUCCESS);
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
                CacheOperationStatus.SUCCESS : CacheOperationStatus.UNKNOWN_CACHE);
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
            removeCacheEntryResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }
        return removeCacheEntryResult;
    }

    @Override
    public PatchValueResult<I, K, V> patchValue(I cacheId, K key, V patch) {
        patchValueResult.clear();
        patchValueResult.getCacheId().copyFrom(cacheId);
        var cache = getCache(cacheId);
        if (cache != null) {
            var result = cache.patchValue(key, patch);
            patchValueResult.getEntryKey().copyFrom(result.getEntryKey());
            patchValueResult.getEntryValue().copyFrom(result.getEntryValue());
            patchValueResult.setStatus(result.getStatus());
        } else {
            patchValueResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }
        return patchValueResult;
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
            getCacheEntryResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }
        return getCacheEntryResult;
    }

    @Override
    public GetAllCacheEntriesResult<I, K, V> getAllCacheEntries(I cacheId) {
        getAllCacheEntriesResult.clear();
        getAllCacheEntriesResult.getCacheId().copyFrom(cacheId);
        var cache = getCache(cacheId);
        if (cache != null) {
            getAllCacheEntriesResult.setStatus(CacheOperationStatus.SUCCESS);
            getAllCacheEntriesResult.setValues(cache.getAllEntries());
        } else {
            getAllCacheEntriesResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }
        return getAllCacheEntriesResult;
    }

    @Override
    public CacheStatsResult getCacheStatsResult() {
        allCacheStatsResult.clear();

        caches.forEach((cacheId, cache) -> {
            var stats = cache.getCacheStats();
            log.info("Got cache stats on cacheId {}", cacheId);
            stats.getCacheId().clear();
            stats.getCacheId().copyFrom(cacheId);
            allCacheStatsResult.getStats().add(stats);
        });

        return allCacheStatsResult;
    }
}
