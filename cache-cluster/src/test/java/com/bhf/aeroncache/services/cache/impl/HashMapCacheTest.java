package com.bhf.aeroncache.services.cache.impl;

import org.junit.jupiter.api.BeforeEach;
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

    /**
     * Test adding a key and value into a cache.
     *
     * @param key   The key to be added.
     * @param value The value to be added.
     */
    @ParameterizedTest
    @MethodSource("provideTestAddParams")
    void testAdd(String key, String value) {
        var result = cache.add(key, value);
        // the result is in the cache
        assertEquals(value, cache.cache.get(key));
        // the result we get back has the correct key
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

    /**
     * Test getting a value from the cache. Included annotations
     * test for null and empty strings.
     *
     * @param key The key to test.
     */
    @ParameterizedTest
    @ValueSource(strings = {"key1"})
    @NullAndEmptySource
    void testGet(String key) {
        var value = seedCache(key, "value");
        var getResult = cache.get(key);
        assertEquals(value, getResult.getEntryValue());
        assertEquals(key, getResult.getEntryKey());
    }


    /**
     * Test removing a value from the cache. Included annotations
     * test for null and empty strings.
     *
     * @param key The key to remove.
     */
    @ParameterizedTest
    @ValueSource(strings = {"key1"})
    @NullAndEmptySource
    void testRemove(String key) {
        var value = seedCache(key, "value");
        var removeResult = cache.remove(key);
        assertEquals(key, removeResult.getKey());
    }

    /**
     * Test clearing the cache.
     */
    @Test
    void testClear() {
        var value = seedCache("key", "value");
        var clearResult = cache.clearEntries();
        assertNotNull(clearResult);
        assertEquals(0, cache.cache.size());
    }

}
