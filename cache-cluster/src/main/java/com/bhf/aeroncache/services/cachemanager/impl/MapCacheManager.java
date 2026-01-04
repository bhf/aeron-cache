package com.bhf.aeroncache.services.cachemanager.impl;

import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.Cache;
import com.bhf.aeroncache.services.cache.CacheEntryCodec;
import com.bhf.aeroncache.services.cache.CacheIdCodec;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
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
public class MapCacheManager<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCacheManager<I, K, V> {

    private final Map<I, Cache<I, K, V>> caches = new Object2ObjectHashMap<>();
    private final Supplier<Map<K, V>> mapSupplier;
    private final CacheIdCodec<I> cacheIdSerializer;
    private final CacheEntryCodec<K, V> cacheEntrySerializer;

    public MapCacheManager(Supplier<I> cacheIndexSupplier, Supplier<K> cacheKeySupplier,
                           Supplier<V> cacheValueSupplier, Supplier<Map<K, V>> mapSupplier,
                           CacheIdCodec<I> cacheIdSerializer,
                           CacheEntryCodec<K, V> cacheEntrySerializer) {
        super(cacheIndexSupplier, cacheKeySupplier, cacheValueSupplier);
        this.mapSupplier = mapSupplier;
        this.cacheIdSerializer = cacheIdSerializer;
        this.cacheEntrySerializer = cacheEntrySerializer;
    }

    @Override
    public void takeSnapshot(ExclusivePublication snapshotPublication) {
        for (var cacheEntry : caches.entrySet()) {
            var cacheId = cacheEntry.getKey();
            var cache = cacheEntry.getValue();
            cache.takeSnapshot(snapshotPublication, cacheId);
        }
    }

    @Override
    public void loadSnapshot(Image snapshotImage) {
        MutableBoolean snapshotFinished = new MutableBoolean(false);

        FragmentHandler handler = (buffer, offset, length, header) -> {
            I cacheId = indexSupplier.get();
            offset = cacheIdSerializer.getCacheId(buffer, offset, cacheId);
            var cacheCreateResult = createCache(cacheId);

            if (cacheCreateResult.getStatus() == OperationStatus.SUCCESS) {
                var cache = getCache(cacheId);
                cache.loadSnapshot(buffer, offset);
            }
            else{
                log.warn("Couldn't create cache on cache Id {}, status: {}", cacheId.value(),
                        cacheCreateResult.getStatus());
            }

        };

        while (!snapshotImage.isEndOfStream()) {
            snapshotImage.poll(handler, 1);
            if (snapshotFinished.value) break;
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

        if (caches.containsKey(cacheId)) {
            cacheCreationResult.setStatus(OperationStatus.CACHE_EXISTS);
            return cacheCreationResult;
        }

        var cache = cacheFactory.getNewCache(indexSupplier, keySupplier, valueSupplier, mapSupplier, cacheIdSerializer, cacheEntrySerializer);
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
            stats.getCacheId().clear();
            stats.getCacheId().copyFrom(cacheId);
            allCacheStatsResult.getStats().add(stats);
        });

        return allCacheStatsResult;
    }
}
