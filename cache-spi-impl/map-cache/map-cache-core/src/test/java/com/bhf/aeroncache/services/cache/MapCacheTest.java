package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.services.integrity.NoOpStreamingHasher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
                new ReusableStringCacheEntrySnapshotCodec(new NoOpStreamingHasher<>()));
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
    @DisplayName("Should take a snapshot of the map cache state")
    void testTakeSnapshot() {
        // Arrange
        seedCache("testKey", "testValue");
        io.aeron.ExclusivePublication snapshotPublication = mock(io.aeron.ExclusivePublication.class);
        var cacheId = new ReusableString();
        cacheId.copyFrom("testCacheId");

        when(snapshotPublication.offer(any(MutableDirectBuffer.class), eq(0), anyInt())).thenReturn(100L);

        // Act
        cache.takeSnapshot(snapshotPublication, cacheId);

        // Assert
        verify(snapshotPublication).offer(any(MutableDirectBuffer.class), eq(0), anyInt());
    }

    @Test
    @DisplayName("Should load a snapshot of the map cache state")
    void testLoadSnapshot() {
        // Arrange
        MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        cache.getCacheStats().size = 0;
        cache.getCacheStats().encode(buffer, 0);

        // Act
        cache.loadSnapshot(buffer, 0);

        // Assert
        assertEquals(0, cache.getAllEntries().size());
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

}
