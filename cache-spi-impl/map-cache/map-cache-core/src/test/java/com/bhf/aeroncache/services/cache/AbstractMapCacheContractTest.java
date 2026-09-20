package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CacheStats;
import com.bhf.aeroncache.models.results.PatchValueResult;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.SnapshotRecords;
import com.bhf.aeroncache.services.integrity.NoOpStreamingHasher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.ExclusivePublication;
import io.aeron.cluster.service.Cluster;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Behaviour shared by every {@link Cache} backed by a map keyed and valued on {@link ReusableString}.
 *
 * <p>Concrete subclasses supply the cache under test via {@link #createCache()} and expose the raw
 * value stored in the backing map via {@link #storedValue(ReusableString)}; the latter is the only
 * place the internal representation (a plain value versus a pooled wrapper) leaks into the tests.
 * Everything else is exercised purely through the {@link Cache} contract.
 *
 * @param <C> The concrete cache type under test.
 */
public abstract class AbstractMapCacheContractTest<C extends Cache<ReusableString, ReusableString, ReusableString>> {

    protected C cache;

    @BeforeEach
    void setup() {
        cache = createCache();
    }

    /**
     * @return A fresh cache instance to exercise.
     */
    protected abstract C createCache();

    /**
     * Read the value currently stored for a key straight out of the backing map, bypassing the
     * {@link Cache} API. Lets subclasses assert on the internal representation.
     *
     * @param key The key to look up.
     * @return The stored value as a string, or {@code null} if the key is absent.
     */
    protected abstract String storedValue(ReusableString key);

    /**
     * Seed the cache with an initial value.
     *
     * @param key   The key to add to the cache.
     * @param value The value to add to the cache.
     * @return The reusable value added to the cache.
     */
    protected ReusableString seedCache(String key, String value) {
        var reusableValue = new ReusableString();
        var reusableKey = new ReusableString();
        reusableKey.copyFrom(key);
        reusableValue.copyFrom(value);
        cache.add(reusableKey, reusableValue);
        return reusableValue;
    }

    protected ReusableString reusable(String value) {
        var reusable = new ReusableString();
        reusable.copyFrom(value);
        return reusable;
    }

    protected PatchValueResult<ReusableString, ReusableString, ReusableString> newMergePatchOut() {
        return new PatchValueResult<>(SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get());
    }

    @Test
    @DisplayName("Should return added key and value and update cache stats")
    void testAdd() {
        // Act
        var result = cache.add(reusable("key"), reusable("value"));

        // Assert
        assertEquals("value", storedValue(reusable("key")));
        assertEquals("key", result.getEntryKey().value());
        assertEquals(1, cache.getCacheStats().addedCount);
        assertEquals(1, cache.getCacheStats().size);
    }

    @Test
    @DisplayName("Should get a known value from seeded key")
    void testGet() {
        // Arrange
        var value = seedCache("key1", "value");

        // Act
        var getResult = cache.get(reusable("key1"));

        // Assert
        assertEquals(value, getResult.getEntryValue());
        assertEquals("key1", getResult.getEntryKey().value());
    }

    @Test
    @DisplayName("Should get unknown key status when key is not in the cache")
    void testGetUnknownKey() {
        // Act
        var getResult = cache.get(reusable("unknownKey"));

        // Assert
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, getResult.getStatus());
    }

    @Test
    @DisplayName("Should remove a known key-value and update cache stats")
    void testRemove() {
        // Arrange
        seedCache("key1", "value");

        // Act
        var removeResult = cache.remove(reusable("key1"));

        // Assert
        assertEquals("key1", removeResult.getKey().value());
        assertTrue(removeResult.isRemoved());
        assertEquals(CacheOperationStatus.SUCCESS, removeResult.getStatus());
        assertEquals(1, cache.getCacheStats().removedCount);
        assertEquals(0, cache.getCacheStats().size);
    }

    @Test
    @DisplayName("Should return unknown key status when removing a missing key")
    void testRemoveUnknownKey() {
        // Act
        var removeResult = cache.remove(reusable("nope"));

        // Assert
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, removeResult.getStatus());
    }

    @Test
    @DisplayName("Should have no entries after clear operation")
    void testClear() {
        // Arrange
        seedCache("key", "value");

        // Act
        var clearResult = cache.clearEntries();

        // Assert
        assertNotNull(clearResult);
        assertEquals(0, cache.getAllEntries().size());
        assertEquals(1, cache.getCacheStats().clearedCount);
    }

    @Test
    @DisplayName("Should return non null cache when getting all entries")
    void testReturnsCacheInstance() {
        // Act
        var allEntries = cache.getAllEntries();

        // Assert
        assertNotNull(allEntries);
    }

    @Test
    @DisplayName("Should snapshot the cache as a CACHE_BEGIN record followed by one CACHE_ENTRY per entry")
    void testTakeSnapshot() {
        // Arrange
        seedCache("testKey", "testValue");
        ExclusivePublication snapshotPublication = mock(ExclusivePublication.class);

        // The snapshot buffer is reused across offers, so capture each record's type as it is offered.
        List<Integer> recordTypes = new ArrayList<>();
        when(snapshotPublication.offer(any(MutableDirectBuffer.class), eq(0), anyInt())).thenAnswer(inv -> {
            MutableDirectBuffer offered = inv.getArgument(0);
            recordTypes.add(offered.getInt(0));
            return 100L;
        });

        // Act
        cache.takeSnapshot(snapshotPublication, reusable("testCacheId"), Mockito.mock(Cluster.class));

        // Assert - one CACHE_BEGIN then one CACHE_ENTRY for the single seeded entry
        assertEquals(List.of(SnapshotRecords.CACHE_BEGIN, SnapshotRecords.CACHE_ENTRY), recordTypes);
    }

    @Test
    @DisplayName("Should apply cache stats from a CACHE_BEGIN record")
    void testApplyStats() {
        // Arrange
        MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        var source = new CacheStats<>(new ReusableString());
        source.size = 7;
        source.addedCount = 7;
        source.encode(buffer, 0);

        // Act
        cache.applyStats(buffer, 0);

        // Assert
        assertEquals(7, cache.getCacheStats().size);
        assertEquals(7, cache.getCacheStats().addedCount);
    }

    @Test
    @DisplayName("Should load a single entry from a CACHE_ENTRY record")
    void testLoadEntry() {
        // Arrange
        var codec = new ReusableStringCacheEntrySnapshotCodec(new NoOpStreamingHasher<>());
        MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        codec.serializeCacheEntry(reusable("k1"), reusable("v1"), buffer, 0);

        // Act
        cache.loadEntry(buffer, 0);

        // Assert
        var entries = cache.getAllEntries();
        assertEquals(1, entries.size());
        assertEquals("v1", entries.get(reusable("k1")).value());
        assertEquals("v1", storedValue(reusable("k1")));
    }

    @Test
    @DisplayName("Should merge patch into an existing JSON value")
    void testPatchValueMergesFields() {
        // Arrange
        seedCache("key1", "{\"a\":1,\"b\":2}");

        // Act
        var result = cache.patchValue(reusable("key1"), reusable("{\"b\":3,\"c\":4}"));

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals("key1", result.getEntryKey().value());
        assertEquals("{\"a\":1,\"b\":3,\"c\":4}", result.getEntryValue().value());
        assertEquals("{\"a\":1,\"b\":3,\"c\":4}", storedValue(reusable("key1")));
    }

    @Test
    @DisplayName("Should deep merge nested JSON objects when patching")
    void testPatchValueDeepMerge() {
        // Arrange
        seedCache("key1", "{\"a\":{\"x\":1}}");

        // Act
        var result = cache.patchValue(reusable("key1"), reusable("{\"a\":{\"y\":2}}"));

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals("{\"a\":{\"x\":1,\"y\":2}}", result.getEntryValue().value());
    }

    @Test
    @DisplayName("Should return unknown key status when patching a missing key")
    void testPatchValueUnknownKey() {
        // Act
        var result = cache.patchValue(reusable("unknownKey"), reusable("{\"a\":1}"));

        // Assert
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, result.getStatus());
    }

    @Test
    @DisplayName("Should return error status when patch is not valid JSON")
    void testPatchValueInvalidJson() {
        // Arrange
        seedCache("key1", "{\"a\":1}");

        // Act
        var result = cache.patchValue(reusable("key1"), reusable("not-json"));

        // Assert
        assertEquals(CacheOperationStatus.ERROR, result.getStatus());
    }

    @Test
    @DisplayName("Should produce a merge patch describing changed and added fields when an add overwrites an existing value")
    void testAddProducesMergePatchOnOverwrite() {
        // Arrange
        seedCache("key1", "{\"a\":1,\"b\":2}");
        var mergePatchOut = newMergePatchOut();

        // Act
        var result = cache.add(reusable("key1"), reusable("{\"a\":1,\"b\":3,\"c\":4}"), mergePatchOut);

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals(CacheOperationStatus.SUCCESS, mergePatchOut.getStatus());
        assertEquals("key1", mergePatchOut.getEntryKey().value());
        assertEquals("{\"b\":3,\"c\":4}", mergePatchOut.getEntryValue().value());
    }

    @Test
    @DisplayName("Should represent a removed field as a null in the merge patch when an add overwrites an existing value")
    void testAddProducesRemovalMergePatch() {
        // Arrange
        seedCache("key1", "{\"a\":1,\"b\":2}");
        var mergePatchOut = newMergePatchOut();

        // Act
        cache.add(reusable("key1"), reusable("{\"a\":1}"), mergePatchOut);

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, mergePatchOut.getStatus());
        assertEquals("{\"b\":null}", mergePatchOut.getEntryValue().value());
    }

    @Test
    @DisplayName("Should not produce a merge patch when an add creates a new key")
    void testAddProducesNoMergePatchOnFirstAdd() {
        // Arrange
        var mergePatchOut = newMergePatchOut();

        // Act
        var result = cache.add(reusable("key1"), reusable("{\"a\":1}"), mergePatchOut);

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals(CacheOperationStatus.NONE, mergePatchOut.getStatus());
    }

    @Test
    @DisplayName("Should not produce a merge patch when an add does not change the value")
    void testAddProducesNoMergePatchWhenValueUnchanged() {
        // Arrange
        seedCache("key1", "{\"a\":1,\"b\":2}");
        var mergePatchOut = newMergePatchOut();

        // Act
        cache.add(reusable("key1"), reusable("{\"a\":1,\"b\":2}"), mergePatchOut);

        // Assert
        assertEquals(CacheOperationStatus.NONE, mergePatchOut.getStatus());
    }

    @Test
    @DisplayName("Should add normally when the merge-patch out-parameter is null")
    void testAddWithNullMergePatchOut() {
        // Arrange
        seedCache("key1", "{\"a\":1}");

        // Act
        var result = cache.add(reusable("key1"), reusable("{\"a\":2}"), null);

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals("{\"a\":2}", storedValue(reusable("key1")));
    }
}
