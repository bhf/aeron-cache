package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
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

class HashMapCacheTest {

    HashMapCache<ReusableLong, ReusableString, ReusableString> cache;

    @BeforeEach
    void setup() {
        cache = new HashMapCache<>(SupplierUtils.longSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
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
    @DisplayName("Should return added key and value")
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
    }

    /**
     * Parameter method source for
     * {@linkplain HashMapCacheTest#testAdd(String, String)}.
     *
     * @return A stream of arguments for the test.
     */
    public static Stream<Arguments> provideTestAddParams() {
        return Stream.of(
                Arguments.of("key", "value"));
    }

    @ParameterizedTest
    @DisplayName("Should get a known value from specified key")
    @ValueSource(strings = {"key1"})
    void testGet(String key) {
        // Arrange
        var value = seedCache(key, "value");
        var reusableKey = new ReusableString();
        reusableKey.copyFrom(key);

        // Act
        cache.add(reusableKey, value);
        var getResult = cache.get(reusableKey);

        // Assert
        assertEquals(value, getResult.getEntryValue());
        assertEquals(key, getResult.getEntryKey().value());
    }

    @ParameterizedTest
    @DisplayName("Should remove a known key-value using specified key")
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
    }

}
