package com.bhf.aeroncache.services.cache.pooled;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.pool.DequeReusableObjectPool;
import com.bhf.aeroncache.pool.ReusableObjectPool;
import com.bhf.aeroncache.services.cache.AbstractCache;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.SnapshotRecords;
import com.bhf.aeroncache.services.patch.ValuePatchProvider;
import io.aeron.ExclusivePublication;
import io.aeron.cluster.service.Cluster;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A cache implementation backed by a Java {@link Map}, like
 * {@link com.bhf.aeroncache.services.cache.MapCache MapCache}, but which pools the objects that make
 * up each entry to minimise allocation and garbage collection.
 *
 * <p>Rather than mapping keys straight to values, the backing map stores {@link PooledMapEntry}
 * wrappers that reference the <em>stored</em> key and value instances. This lets a removal recover
 * the stored key (which a plain {@code Map.remove} does not hand back) so that the key, the value
 * and the wrapper itself can all be released back to their pools and reused. Three pools are held:
 * one for keys, one for values and one for the entry wrappers.
 *
 * <p>The {@link Cache} contract's {@link #getAllEntries()} still exposes a {@code Map<K, V>}: a
 * lightweight {@link PooledEntryMapView view} is returned that unwraps values on the fly, so
 * snapshotting, bulk reads and the counters path all see plain values without the pooling being
 * visible to them.
 *
 * @param <I> The type the cache is indexed on.
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
@Log4j2
public class PooledMapCache<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCache<I, K, V> {

    /**
     * Default number of instances pre-created in each pool when the convenience constructor is used.
     */
    public static final int DEFAULT_INITIAL_POOL_SIZE = 16;

    final Map<K, PooledMapEntry<K, V>> cache;
    private final V emptyValue;
    private final V previousValueScratch;
    private final CacheIdSnapshotCodec<I> cacheIdSnapshotCodec;
    private final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
    private final CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec;
    private final ValuePatchProvider<I, K, V> patchProvider;

    final ReusableObjectPool<K> keyPool;
    final ReusableObjectPool<V> valuePool;
    final ReusableObjectPool<PooledMapEntry<K, V>> entryPool;
    private final Map<K, V> entriesView;

    /**
     * Create a pooled cache using the {@link #DEFAULT_INITIAL_POOL_SIZE default pool size} and
     * recycling instances created on demand.
     */
    public PooledMapCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier,
                          Supplier<Map<K, PooledMapEntry<K, V>>> mapSupplier, CacheIdSnapshotCodec<I> cacheIdSnapshotCodec,
                          CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec, ValuePatchProvider<I, K, V> patchProvider) {
        this(indexSupplier, keySupplier, valueSupplier, mapSupplier, cacheIdSnapshotCodec, cacheEntrySnapshotCodec,
                patchProvider, DEFAULT_INITIAL_POOL_SIZE, true);
    }

    /**
     * Create a pooled cache.
     *
     * @param initialPoolSize   Number of instances to pre-create in each of the key, value and entry
     *                          pools.
     * @param recycleNewObjects Whether instances created on demand once a pool is empty are retained
     *                          when released; see {@link DequeReusableObjectPool}.
     */
    public PooledMapCache(Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier,
                          Supplier<Map<K, PooledMapEntry<K, V>>> mapSupplier, CacheIdSnapshotCodec<I> cacheIdSnapshotCodec,
                          CacheEntrySnapshotCodec<K, V> cacheEntrySnapshotCodec, ValuePatchProvider<I, K, V> patchProvider,
                          int initialPoolSize, boolean recycleNewObjects) {
        super(indexSupplier, keySupplier, valueSupplier);
        this.cache = mapSupplier.get();
        this.emptyValue = valueSupplier.get();
        this.previousValueScratch = valueSupplier.get();
        this.cacheIdSnapshotCodec = cacheIdSnapshotCodec;
        this.cacheEntrySnapshotCodec = cacheEntrySnapshotCodec;
        this.patchProvider = patchProvider;
        this.keyPool = new DequeReusableObjectPool<>(keySupplier, initialPoolSize, recycleNewObjects);
        this.valuePool = new DequeReusableObjectPool<>(valueSupplier, initialPoolSize, recycleNewObjects);
        this.entryPool = new DequeReusableObjectPool<>(PooledMapEntry<K, V>::new, initialPoolSize, recycleNewObjects);
        this.entriesView = new PooledEntryMapView();
    }

    @Override
    public AddCacheEntryResult<I, K> add(K key, V value) {
        addCacheEntryResult.clear();
        addCacheEntryResult.getEntryKey().copyFrom(key);
        putPooled(key, value);
        addCacheEntryResult.setEntryAdded(true);
        addCacheEntryResult.setStatus(CacheOperationStatus.SUCCESS);
        stats.addedCount++;
        stats.size = cache.size();
        return addCacheEntryResult;
    }

    @Override
    public AddCacheEntryResult<I, K> add(K key, V value, PatchValueResult<I, K, V> mergePatchOut) {
        V previousValue = null;
        if (mergePatchOut != null) {
            PooledMapEntry<K, V> existing = cache.get(key);
            if (existing != null) {
                // Copy the previous value aside: putPooled overwrites the stored value in place, so
                // the live instance can no longer stand in for its pre-add state.
                previousValueScratch.clear();
                previousValueScratch.copyFrom(existing.getValue());
                previousValue = previousValueScratch;
            }
        }
        var result = add(key, value);
        if (mergePatchOut != null) {
            produceMergePatch(key, previousValue, value, mergePatchOut);
        }
        return result;
    }

    private void produceMergePatch(K key, V previousValue, V newValue, PatchValueResult<I, K, V> mergePatchOut) {
        patchProvider.produceMergePatch(key, previousValue, newValue, mergePatchOut);
    }

    /**
     * Insert or overwrite an entry using pooled instances. An overwrite reuses the existing stored
     * key, value and wrapper, mutating the value in place; a new entry acquires a key, value and
     * wrapper from the pools.
     */
    private void putPooled(K key, V value) {
        PooledMapEntry<K, V> existing = cache.get(key);
        if (existing != null) {
            existing.getValue().clear();
            existing.getValue().copyFrom(value);
            return;
        }
        K pooledKey = keyPool.acquire();
        pooledKey.copyFrom(key);
        V pooledValue = valuePool.acquire();
        pooledValue.copyFrom(value);
        PooledMapEntry<K, V> entry = entryPool.acquire();
        entry.set(pooledKey, pooledValue);
        cache.put(pooledKey, entry);
    }

    @Override
    public GetCacheEntryResult<I, K, V> get(K key) {
        getCacheEntryResult.clear();
        getCacheEntryResult.getEntryKey().copyFrom(key);

        PooledMapEntry<K, V> entry = cache.get(key);
        if (entry != null) {
            getCacheEntryResult.getEntryValue().copyFrom(entry.getValue());
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

        PooledMapEntry<K, V> entry = cache.get(key);
        if (entry == null) {
            patchValueResult.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            return patchValueResult;
        }

        V existingValue = entry.getValue();
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
        PooledMapEntry<K, V> entry = cache.remove(key);
        boolean removed = entry != null;
        if (removed) {
            recycleEntry(entry);
        }
        removeCacheEntryResult.setRemoved(removed);
        removeCacheEntryResult.setStatus(removed ?
                CacheOperationStatus.SUCCESS : CacheOperationStatus.UNKNOWN_KEY);
        stats.removedCount++;
        stats.size = cache.size();
        return removeCacheEntryResult;
    }

    @Override
    public ClearCacheResult<I> clearEntries() {
        clearCacheResult.clear();
        for (PooledMapEntry<K, V> entry : cache.values()) {
            recycleEntry(entry);
        }
        cache.clear();
        clearCacheResult.setStatus(CacheOperationStatus.SUCCESS);
        stats.clearedCount++;
        stats.size = 0;
        return clearCacheResult;
    }

    /**
     * Release an entry's stored key and value back to their pools, then release the wrapper itself.
     * The key and value must be released before the wrapper, as releasing the wrapper clears the
     * references to them.
     */
    private void recycleEntry(PooledMapEntry<K, V> entry) {
        keyPool.release(entry.getKey());
        valuePool.release(entry.getValue());
        entryPool.release(entry);
    }

    @Override
    public Map<K, V> getAllEntries() {
        return entriesView;
    }

    @Override
    public void takeSnapshot(ExclusivePublication snapshotPublication, I cacheId, Cluster cluster) {
        offerCacheBegin(snapshotPublication, cacheId, cluster);

        var sortedKeys = getSortedKeys();
        int entriesSnapshotted = 0;

        for (var key : sortedKeys) {
            ++entriesSnapshotted;
            var value = cache.get(key).getValue();
            offerCacheEntry(snapshotPublication, key, value, cluster);

            if (entriesSnapshotted % 100 == 0) {
                cluster.idleStrategy().idle();
            }
        }

        log.info("Snapshot for cache {} sent successfully with {} entries", cacheId, entriesSnapshotted);
    }

    private void offerCacheBegin(ExclusivePublication snapshotPublication, I cacheId, Cluster cluster) {
        int offset = SnapshotRecords.TYPE_LENGTH;
        offset = cacheIdSnapshotCodec.serializeCacheId(cacheId, buffer, offset);
        offset = stats.encode(buffer, offset);
        buffer.putInt(0, SnapshotRecords.CACHE_BEGIN);
        offerRecord(snapshotPublication, offset, cluster);
    }

    private void offerCacheEntry(ExclusivePublication snapshotPublication, K key, V value, Cluster cluster) {
        int offset = SnapshotRecords.TYPE_LENGTH;
        offset = cacheEntrySnapshotCodec.serializeCacheEntry(key, value, buffer, offset);
        buffer.putInt(0, SnapshotRecords.CACHE_ENTRY);
        offerRecord(snapshotPublication, offset, cluster);
    }

    private void offerRecord(ExclusivePublication snapshotPublication, int length, Cluster cluster) {
        while (snapshotPublication.offer(buffer, 0, length) < 0) {
            cluster.idleStrategy().idle();
        }
    }

    private List<K> getSortedKeys() {
        List<K> res = new ArrayList<>(cache.keySet());
        Comparator<K> keyComparator = cacheEntrySnapshotCodec.getKeyComparator();
        res.sort(keyComparator);
        return res;
    }

    @Override
    public int applyStats(DirectBuffer buffer, int offset) {
        offset = stats.decode(buffer, offset);
        log.info("Loaded stats for cache Id: {}, size: {}, added: {}", stats.getCacheId(), stats.size, stats.addedCount);
        return offset;
    }

    @Override
    public int loadEntry(DirectBuffer buffer, int offset) {
        // The codec writes decoded entries via Map.put; the view routes those puts through the pools.
        return cacheEntrySnapshotCodec.deserializeCacheEntry(buffer, offset, entriesView);
    }

    /**
     * A {@link Map} facade over the backing {@link PooledMapEntry} map that unwraps values, so
     * callers see a plain {@code Map<K, V>}. Reads unwrap on the fly; {@link #put(Reusable, Reusable)
     * puts} are routed through the pools so that snapshot loading reuses pooled instances too.
     */
    private final class PooledEntryMapView extends AbstractMap<K, V> {

        @Override
        public V get(Object key) {
            PooledMapEntry<K, V> entry = cache.get(key);
            return entry == null ? null : entry.getValue();
        }

        @Override
        public boolean containsKey(Object key) {
            return cache.containsKey(key);
        }

        @Override
        public int size() {
            return cache.size();
        }

        @Override
        public V put(K key, V value) {
            PooledMapEntry<K, V> existing = cache.get(key);
            V previous = existing == null ? null : existing.getValue();
            putPooled(key, value);
            return previous;
        }

        @Override
        public Set<K> keySet() {
            return cache.keySet();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    final Iterator<Entry<K, PooledMapEntry<K, V>>> it = cache.entrySet().iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<K, V> next() {
                            // Read key and value out immediately: some Map implementations (e.g.
                            // Agrona's Object2ObjectHashMap) reuse a single Entry flyweight per step.
                            Entry<K, PooledMapEntry<K, V>> next = it.next();
                            return new SimpleImmutableEntry<>(next.getKey(), next.getValue().getValue());
                        }
                    };
                }

                @Override
                public int size() {
                    return cache.size();
                }
            };
        }
    }
}
