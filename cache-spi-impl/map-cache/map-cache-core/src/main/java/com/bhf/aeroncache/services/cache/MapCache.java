package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class MapCache<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCache<I, K, V> {

    final Map<K, V> cache;
    private final V emptyValue;
    private final CacheIdSnapshotCodec<I> cacheIdSnapshotCodec;
    private final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
    private final CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MapCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier,
                    Supplier<Map<K, V>> mapSupplier, CacheIdSnapshotCodec<I> cacheIdSnapshotCodec,
                    CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec) {
        super(indexSupplier, keySupplier, valueSupplier);
        this.cache = mapSupplier.get();
        this.emptyValue = valueSupplier.get();
        this.cacheIdSnapshotCodec = cacheIdSnapshotCodec;
        this.cacheEntrySnapshotCodec = cacheEntrySnapshotCodec;
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

    /**
     * Produce an RFC 7386 JSON merge patch describing the change from a pre-existing
     * value to the newly added value, populating the supplied out-parameter. If there
     * was no previous value or the values are equal, the out-parameter is left cleared
     * (status {@link CacheOperationStatus#NONE}) so no patch update is sent.
     */
    private void produceMergePatch(K key, V previousValue, V newValue, PatchValueResult<I, K, V> mergePatchOut) {
        mergePatchOut.clear();
        if (previousValue == null) {
            return;
        }
        mergePatchOut.getEntryKey().copyFrom(key);
        try {
            JsonNode oldNode = objectMapper.readTree(previousValue.value().toString());
            JsonNode newNode = objectMapper.readTree(newValue.value().toString());
            JsonNode patchNode = computeMergePatch(oldNode, newNode);

            if (patchNode.isObject() && patchNode.isEmpty()) {
                // nothing changed, no patch update required
                return;
            }

            String patchJson = objectMapper.writeValueAsString(patchNode);
            mergePatchOut.getEntryValue().copyFrom(patchJson);
            mergePatchOut.setStatus(CacheOperationStatus.SUCCESS);
        } catch (JsonProcessingException e) {
            log.warn("Failed to compute merge patch for key {}, reason: {}", key, e.getMessage());
            mergePatchOut.setStatus(CacheOperationStatus.ERROR);
        }
    }

    /**
     * Compute an RFC 7386 JSON merge patch that transforms {@code source} into {@code target}.
     */
    private JsonNode computeMergePatch(JsonNode source, JsonNode target) {
        if (!source.isObject() || !target.isObject()) {
            return target;
        }

        ObjectNode patch = objectMapper.createObjectNode();

        var sourceFields = source.fieldNames();
        while (sourceFields.hasNext()) {
            String field = sourceFields.next();
            if (!target.has(field)) {
                patch.set(field, NullNode.getInstance());
            }
        }

        var targetFields = target.fieldNames();
        while (targetFields.hasNext()) {
            String field = targetFields.next();
            JsonNode targetValue = target.get(field);
            if (!source.has(field)) {
                patch.set(field, targetValue);
            } else {
                JsonNode sourceValue = source.get(field);
                if (sourceValue.isObject() && targetValue.isObject()) {
                    JsonNode nested = computeMergePatch(sourceValue, targetValue);
                    if (!nested.isObject() || !nested.isEmpty()) {
                        patch.set(field, nested);
                    }
                } else if (!sourceValue.equals(targetValue)) {
                    patch.set(field, targetValue);
                }
            }
        }

        return patch;
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
            String existingJson = existingValue.value().toString();
            String patchJson = patch.value().toString();
            JsonNode mergedNode = objectMapper.readerForUpdating(objectMapper.readTree(existingJson)).readValue(patchJson);
            String mergedJson = objectMapper.writeValueAsString(mergedNode);

            existingValue.clear();
            existingValue.copyFrom(mergedJson);

            patchValueResult.getEntryValue().copyFrom(existingValue);
            patchValueResult.setStatus(CacheOperationStatus.SUCCESS);
        } catch (JsonProcessingException e) {
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
