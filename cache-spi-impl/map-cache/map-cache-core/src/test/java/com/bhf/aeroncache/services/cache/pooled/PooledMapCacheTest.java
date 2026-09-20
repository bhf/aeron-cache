package com.bhf.aeroncache.services.cache.pooled;

import com.bhf.aeroncache.services.cache.AbstractMapCacheContractTest;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.services.integrity.NoOpStreamingHasher;
import com.bhf.aeroncache.services.patch.JSONPatchProvider;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import org.agrona.collections.Object2ObjectHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link PooledMapCache} against the shared {@link AbstractMapCacheContractTest cache
 * contract}, plus the pooling behaviour that is unique to this implementation.
 */
class PooledMapCacheTest extends AbstractMapCacheContractTest<PooledMapCache<ReusableString, ReusableString, ReusableString>> {

    static final Supplier<Map<ReusableString, PooledMapEntry<ReusableString, ReusableString>>> pooledMapSupplier =
            Object2ObjectHashMap::new;

    @Override
    protected PooledMapCache<ReusableString, ReusableString, ReusableString> createCache() {
        return createCache(PooledMapCache.DEFAULT_INITIAL_POOL_SIZE, true);
    }

    private PooledMapCache<ReusableString, ReusableString, ReusableString> createCache(int initialPoolSize,
                                                                                       boolean recycleNewObjects) {
        return new PooledMapCache<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier,
                SupplierUtils.stringSupplier, pooledMapSupplier,
                new ReusableStringCacheIdSnapshotCodec(new NoOpStreamingHasher<>()),
                new ReusableStringCacheEntrySnapshotCodec(new NoOpStreamingHasher<>()), new JSONPatchProvider<>(),
                initialPoolSize, recycleNewObjects);
    }

    @Override
    protected String storedValue(ReusableString key) {
        var entry = cache.cache.get(key);
        return entry == null ? null : entry.getValue().value();
    }

    @Test
    @DisplayName("Should overwrite the value in place when adding an existing key")
    void testAddOverwriteReusesEntry() {
        // Arrange
        seedCache("key1", "value1");
        var storedEntry = cache.cache.get(reusable("key1"));

        // Act
        cache.add(reusable("key1"), reusable("value2"));

        // Assert - same wrapper instance retained, value updated in place, no extra entry
        assertEquals(1, cache.cache.size());
        assertEquals("value2", cache.cache.get(reusable("key1")).getValue().value());
        assertSame(storedEntry, cache.cache.get(reusable("key1")));
    }

    @Test
    @DisplayName("Should recycle key, value and wrapper back to the pools on remove")
    void testRemoveRecyclesToPools() {
        // Arrange - start with empty pools so a remove is the only thing that can populate them.
        cache = createCache(0, true);
        seedCache("key1", "value1");

        // Act
        cache.remove(reusable("key1"));

        // Assert - the key, value and wrapper are all released back and reusable.
        var recycledKey = cache.keyPool.acquire();
        var recycledValue = cache.valuePool.acquire();
        var recycledEntry = cache.entryPool.acquire();
        assertNotNull(recycledKey);
        assertNotNull(recycledValue);
        assertNotNull(recycledEntry);
        // A cleared string is empty, proving the instances were cleared on release.
        assertEquals("", recycledKey.value());
        assertEquals("", recycledValue.value());
    }

    @Test
    @DisplayName("Should expose a plain unwrapped value view from getAllEntries")
    void testGetAllEntriesUnwrapsValues() {
        // Arrange
        seedCache("key1", "value1");
        seedCache("key2", "value2");

        // Act
        var allEntries = cache.getAllEntries();

        // Assert
        assertEquals(2, allEntries.size());
        assertEquals("value1", allEntries.get(reusable("key1")).value());
        assertEquals("value2", allEntries.get(reusable("key2")).value());

        // entrySet iteration yields unwrapped values
        var collected = new ArrayList<String>();
        for (var entry : allEntries.entrySet()) {
            collected.add(entry.getValue().value());
        }
        assertEquals(2, collected.size());
        assertTrue(collected.contains("value1"));
        assertTrue(collected.contains("value2"));
    }
}
