package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CacheStats;
import com.bhf.aeroncache.models.results.PatchValueResult;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.SnapshotRecords;
import com.bhf.aeroncache.services.integrity.NoOpStreamingHasher;
import com.bhf.aeroncache.services.patch.JSONPatchProvider;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.ExclusivePublication;
import io.aeron.cluster.service.Cluster;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MapCacheTest {

    MapCache<ReusableString, ReusableString, ReusableString> cache;

    @BeforeEach
    void setup() {
        cache = new MapCache<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier,
                SupplierUtils.stringSupplier, SupplierUtils.mapSupplier,
                new ReusableStringCacheIdSnapshotCodec(new NoOpStreamingHasher<>()),
                new ReusableStringCacheEntrySnapshotCodec(new NoOpStreamingHasher<>()), new JSONPatchProvider<>());
    }

    /**
     * Seed the cache with some initial values.
     *
     * @param key   The key to add to the cache.
     * @param value The value to add to the cache.
     * @return The value added to the cache.
     */
    private ReusableString seedCache(String key, String value) {
        var reusableValue = new ReusableString();
        var reusableKey = new ReusableString();
        reusableKey.copyFrom(key);
        reusableValue.copyFrom(value);
        cache.add(reusableKey,reusableValue);
        return reusableValue;
    }

    @ParameterizedTest
    @DisplayName("Should return added key and value and update cache stats")
    @MethodSource("provideTestAddParams")
    void testAdd(String key, String value) {
        // Arrange
        var reusableKey = new ReusableString();
        var reusableValue = new ReusableString();
        reusableKey.copyFrom(key);
        reusableValue.copyFrom(value);

        // Act
        var result = cache.add(reusableKey, reusableValue);

        // Assert
        assertEquals(value, cache.cache.get(reusableKey).value());
        assertEquals(key, result.getEntryKey().value());
        assertEquals(1, cache.getCacheStats().addedCount);
        assertEquals(1, cache.getCacheStats().size);
    }

    /**
     * Parameter method source for
     * {@linkplain MapCacheTest#testAdd(String, String)}.
     *
     * @return A stream of arguments for the test.
     */
    public static Stream<Arguments> provideTestAddParams() {
        return Stream.of(
                Arguments.of("key", "value"));
    }

    @ParameterizedTest
    @DisplayName("Should get a known value from seeded key")
    @ValueSource(strings = {"key1"})
    void testGet(String key) {
        // Arrange
        var value = seedCache(key, "value");
        var reusableKey = new ReusableString();
        reusableKey.copyFrom(key);

        // Act
        var getResult = cache.get(reusableKey);

        // Assert
        assertEquals(value, getResult.getEntryValue());
        assertEquals(key, getResult.getEntryKey().value());
    }

    @ParameterizedTest
    @DisplayName("Should get unknown key status when key is not in the cache")
    @ValueSource(strings = {"unknownKey"})
    void testGetUnknownKey(String key) {
        // Arrange
        var reusableKey = new ReusableString();
        reusableKey.copyFrom(key);

        // Act
        var getResult = cache.get(reusableKey);

        // Assert
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, getResult.getStatus());
    }

    @ParameterizedTest
    @DisplayName("Should remove a known key-value and update cache stats")
    @ValueSource(strings = {"key1"})
    void testRemove(String key) {
        // Arrange
        var value = seedCache(key, "value");
        var reusableKey = new ReusableString();
        reusableKey.copyFrom(key);

        // Act
        var removeResult = cache.remove(reusableKey);

        // Assert
        assertEquals(key, removeResult.getKey().value());
        assertEquals(1, cache.getCacheStats().removedCount);
        assertEquals(0, cache.getCacheStats().size);
    }

    @Test
    @DisplayName("Should have no entries after clear operation")
    void testClear() {
        // Arrange
        var value = seedCache("key", "value");

        // Act
        var clearResult = cache.clearEntries();

        // Assert
        assertNotNull(clearResult);
        assertEquals(0, cache.cache.size());
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
        var cacheId = new ReusableString();
        cacheId.copyFrom("testCacheId");

        // The snapshot buffer is reused across offers, so capture each record's type as it is offered.
        List<Integer> recordTypes = new ArrayList<>();
        when(snapshotPublication.offer(any(MutableDirectBuffer.class), eq(0), anyInt())).thenAnswer(inv -> {
            MutableDirectBuffer offered = inv.getArgument(0);
            recordTypes.add(offered.getInt(0));
            return 100L;
        });

        // Act
        cache.takeSnapshot(snapshotPublication, cacheId, Mockito.mock(Cluster.class));

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
        var key = new ReusableString();
        key.copyFrom("k1");
        var value = new ReusableString();
        value.copyFrom("v1");
        codec.serializeCacheEntry(key, value, buffer, 0);

        // Act
        cache.loadEntry(buffer, 0);

        // Assert
        var entries = cache.getAllEntries();
        assertEquals(1, entries.size());
        assertEquals("v1", entries.get(key).value());
    }

    @Test
    @DisplayName("Should merge patch into an existing JSON value")
    void testPatchValueMergesFields() {
        // Arrange
        seedCache("key1", "{\"a\":1,\"b\":2}");
        var reusableKey = new ReusableString();
        reusableKey.copyFrom("key1");
        var patch = new ReusableString();
        patch.copyFrom("{\"b\":3,\"c\":4}");

        // Act
        var result = cache.patchValue(reusableKey, patch);

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals("key1", result.getEntryKey().value());
        assertEquals("{\"a\":1,\"b\":3,\"c\":4}", result.getEntryValue().value());
        assertEquals("{\"a\":1,\"b\":3,\"c\":4}", cache.cache.get(reusableKey).value());
    }

    @Test
    @DisplayName("Should deep merge nested JSON objects when patching")
    void testPatchValueDeepMerge() {
        // Arrange
        seedCache("key1", "{\"a\":{\"x\":1}}");
        var reusableKey = new ReusableString();
        reusableKey.copyFrom("key1");
        var patch = new ReusableString();
        patch.copyFrom("{\"a\":{\"y\":2}}");

        // Act
        var result = cache.patchValue(reusableKey, patch);

        // Assert
        assertEquals(CacheOperationStatus.SUCCESS, result.getStatus());
        assertEquals("{\"a\":{\"x\":1,\"y\":2}}", result.getEntryValue().value());
    }

    @Test
    @DisplayName("Should return unknown key status when patching a missing key")
    void testPatchValueUnknownKey() {
        // Arrange
        var reusableKey = new ReusableString();
        reusableKey.copyFrom("unknownKey");
        var patch = new ReusableString();
        patch.copyFrom("{\"a\":1}");

        // Act
        var result = cache.patchValue(reusableKey, patch);

        // Assert
        assertEquals(CacheOperationStatus.UNKNOWN_KEY, result.getStatus());
    }

    @Test
    @DisplayName("Should return error status when patch is not valid JSON")
    void testPatchValueInvalidJson() {
        // Arrange
        seedCache("key1", "{\"a\":1}");
        var reusableKey = new ReusableString();
        reusableKey.copyFrom("key1");
        var patch = new ReusableString();
        patch.copyFrom("not-json");

        // Act
        var result = cache.patchValue(reusableKey, patch);

        // Assert
        assertEquals(CacheOperationStatus.ERROR, result.getStatus());
    }


    private PatchValueResult<ReusableString, ReusableString, ReusableString> newMergePatchOut() {
        return new PatchValueResult<>(SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get(),
                SupplierUtils.stringSupplier.get());
    }

    private ReusableString reusable(String value) {
        var reusable = new ReusableString();
        reusable.copyFrom(value);
        return reusable;
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
        assertEquals("{\"a\":2}", cache.cache.get(reusable("key1")).value());
    }

}
