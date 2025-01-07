package com.bhf.aeroncache.services.cache.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HashMapCacheTest {

    HashMapCache<Long, String, String> cache;

    @BeforeEach
    void setup() {
        cache = new HashMapCache<>();
    }

    /**
     * Seed the cache with some initial values.
     *
     * @param key   The key to add to the cache.
     * @param value The value to add to the cache.
     * @return The value added to the cache.
     */
    private String seedCache(String key, String value) {
        cache.add(key, value);
        return value;
    }

    @ParameterizedTest
    @DisplayName("Should return added key and value")
    @MethodSource("provideTestAddParams")
    void testAdd(String key, String value) {
        // Arrange

        // Act
        var result = cache.add(key, value);

        // Assert
        assertEquals(value, cache.cache.get(key));
        assertEquals(key, result.getEntryKey());
    }

    /**
     * Parameter method source for
     * {@linkplain HashMapCacheTest#testAdd(String, String)}.
     *
     * @return A stream of arguments for the test.
     */
    public static Stream<Arguments> provideTestAddParams() {
        return Stream.of(Arguments.of(null, null),
                Arguments.of(null, "value"),
                Arguments.of("key", null),
                Arguments.of("key", "value"));
    }

    @ParameterizedTest
    @DisplayName("Should get a known value from specified key")
    @ValueSource(strings = {"key1"})
    @NullAndEmptySource
    void testGet(String key) {
        // Arrange
        var value = seedCache(key, "value");

        // Act
        var getResult = cache.get(key);

        // Assert
        assertEquals(value, getResult.getEntryValue());
        assertEquals(key, getResult.getEntryKey());
    }

    @ParameterizedTest
    @DisplayName("Should remove a known key-value using specified key")
    @ValueSource(strings = {"key1"})
    @NullAndEmptySource
    void testRemove(String key) {
        // Arrange
        var value = seedCache(key, "value");

        // Act
        var removeResult = cache.remove(key);

        // Assert
        assertEquals(key, removeResult.getKey());
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
