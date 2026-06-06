package com.bhf.aeroncache.models;

import lombok.Getter;
import lombok.Setter;
import java.util.Objects;

/**
 * A compound key for identifying a particular TTL timer.
 *
 * @param <I> The type of the cache ID.
 * @param <K> The type of the key.
 */
@Getter
@Setter
public class TimerLookupCompoundKey<I extends Reusable, K extends Reusable> implements Reusable<TimerLookupCompoundKey<I, K>> {
    private final I cacheId;
    private final K key;

    public TimerLookupCompoundKey(I cacheId, K key) {
        this.cacheId = cacheId;
        this.key = key;
    }

    @Override
    public void clear() {
        cacheId.clear();
        key.clear();
    }

    @Override
    public void copyFrom(TimerLookupCompoundKey<I, K> source) {
        this.key.copyFrom(source.key);
        this.cacheId.copyFrom(source.cacheId);
    }

    @Override
    public void copyFrom(Reusable<TimerLookupCompoundKey<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public TimerLookupCompoundKey<I, K> value() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimerLookupCompoundKey<?, ?> other = (TimerLookupCompoundKey<?, ?>) o;
        return Objects.equals(cacheId, other.cacheId) && Objects.equals(key, other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cacheId, key);
    }

    @Override
    public String toString() {
        return "CacheKeyCompound{" +
                "cacheId=" + cacheId +
                ", key=" + key +
                '}';
    }
}
