package com.bhf.aeroncache.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompoundCacheKeyTest {

    /**
     * Minimal mutable {@link Reusable} test double backing a String value, mirroring
     * the reuse semantics of the production ReusableString type without pulling in
     * cache-common.
     */
    private static final class TestKey implements Reusable<String> {
        private String value = "";

        TestKey() {
        }

        TestKey(String value) {
            this.value = value;
        }

        @Override
        public void clear() {
            value = "";
        }

        @Override
        public void copyFrom(String source) {
            this.value = source;
        }

        @Override
        public void copyFrom(Reusable<String> source) {
            this.value = source.value();
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value.equals(((TestKey) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    private static CompoundCacheKey<TestKey, TestKey> key(String cacheId, String key) {
        return new CompoundCacheKey<>(new TestKey(cacheId), new TestKey(key));
    }

    @Test
    void shouldBeEqualForSameCacheIdAndKey() {
        var a = key("cache1", "k1");
        var b = key("cache1", "k1");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentKey() {
        assertNotEquals(key("cache1", "k1"), key("cache1", "k2"));
    }

    @Test
    void shouldNotBeEqualForDifferentCacheId() {
        assertNotEquals(key("cache1", "k1"), key("cache2", "k1"));
    }

    @Test
    void shouldBeEqualToItself() {
        var a = key("cache1", "k1");
        assertEquals(a, a);
    }

    @Test
    void shouldNotBeEqualToNullOrOtherType() {
        var a = key("cache1", "k1");
        assertNotEquals(a, null);
        assertNotEquals(a, "cache1:k1");
    }

    @Test
    void clearShouldResetBothComponents() {
        var a = key("cache1", "k1");
        a.clear();

        assertEquals("", a.getCacheId().value());
        assertEquals("", a.getKey().value());
    }

    @Test
    void clearedKeyActsAsWholeCacheSentinel() {
        // Whole-cache subscription: clear only the key, retain the cacheId.
        var wholeCache = key("cache1", "k1");
        wholeCache.getKey().clear();

        var expectedSentinel = key("cache1", "");

        assertEquals(expectedSentinel, wholeCache);
        assertEquals(expectedSentinel.hashCode(), wholeCache.hashCode());
    }

    @Test
    void copyFromShouldMakeKeysEqual() {
        var source = key("cacheA", "kA");
        var target = key("cacheB", "kB");

        target.copyFrom(source);

        assertEquals(source, target);
        assertEquals("cacheA", target.getCacheId().value());
        assertEquals("kA", target.getKey().value());
    }

    @Test
    void copyFromReusableShouldMakeKeysEqual() {
        var source = key("cacheA", "kA");
        var target = key("cacheB", "kB");

        target.copyFrom((Reusable<CompoundCacheKey<TestKey, TestKey>>) source);

        assertEquals(source, target);
    }

    @Test
    void valueShouldReturnSelf() {
        var a = key("cache1", "k1");
        assertSame(a, a.value());
    }
}
