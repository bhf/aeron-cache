package com.bhf.aeroncache.services.cache.pooled;

import com.bhf.aeroncache.models.Reusable;

/**
 * A poolable flyweight that holds references to a cache entry's key and value.
 *
 * <p>Instances of this class are the values stored in {@link PooledMapCache}'s backing map. Because
 * the wrapper keeps a reference to the <em>stored</em> key instance (the one actually held by the
 * map, not the lookup key passed by a caller), a removal can recover and recycle both the key and
 * the value back to their respective pools. A plain {@code Map.remove} only returns the value, so
 * the stored key would otherwise be left for the garbage collector.
 *
 * <p>The wrapper itself is {@link Reusable} so it too can be pooled. {@link #clear()} only drops the
 * references it holds; the referenced key and value are recycled independently via their own pools
 * before the wrapper is released.
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
public class PooledMapEntry<K extends Reusable, V extends Reusable> implements Reusable<PooledMapEntry<K, V>> {

    private K key;
    private V value;

    /**
     * @return The stored key instance this wrapper references, or {@code null} if unset/cleared.
     */
    public K getKey() {
        return key;
    }

    /**
     * @return The stored value instance this wrapper references, or {@code null} if unset/cleared.
     */
    public V getValue() {
        return value;
    }

    /**
     * Point this wrapper at a key and value pair.
     *
     * @param key   The stored key instance.
     * @param value The stored value instance.
     */
    public void set(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void clear() {
        this.key = null;
        this.value = null;
    }

    @Override
    public void copyFrom(PooledMapEntry<K, V> source) {
        this.key = source.key;
        this.value = source.value;
    }

    @Override
    public void copyFrom(Reusable<PooledMapEntry<K, V>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public PooledMapEntry<K, V> value() {
        return this;
    }
}
