package com.bhf.aeroncache.models;

import lombok.Getter;
import lombok.Setter;
import java.util.Objects;

/**
 * A reusable compound key pairing a cache ID with a key.
 *
 * <p>Used wherever a value needs to be looked up or grouped by the
 * {@code (cacheId, key)} pair, for example TTL timer bookkeeping and
 * key-based subscription fan-out. An {@link Reusable#clear() cleared} key
 * acts as the "no specific key" / whole-cache sentinel.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key.
 */
@Getter
@Setter
public class CompoundCacheKey<I extends Reusable, K extends Reusable> implements Reusable<CompoundCacheKey<I, K>> {
    private final I cacheId;
    private final K key;

    public CompoundCacheKey(I cacheId, K key) {
        this.cacheId = cacheId;
        this.key = key;
    }

    @Override
    public void clear() {
        cacheId.clear();
        key.clear();
    }

    @Override
    public void copyFrom(CompoundCacheKey<I, K> source) {
        this.key.copyFrom(source.key);
        this.cacheId.copyFrom(source.cacheId);
    }

    @Override
    public void copyFrom(Reusable<CompoundCacheKey<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CompoundCacheKey<I, K> value() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompoundCacheKey<?, ?> other = (CompoundCacheKey<?, ?>) o;
        return Objects.equals(cacheId, other.cacheId) && Objects.equals(key, other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cacheId, key);
    }

    @Override
    public String toString() {
        return "CompoundCacheKey{" +
                "cacheId=" + cacheId +
                ", key=" + key +
                '}';
    }
}
