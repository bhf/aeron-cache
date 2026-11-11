package com.bhf.aeroncache.near;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * A single near (local) cache.
 */
public interface NearCache<V> {

    V get(String key);

    boolean containsKey(String key);

    void put(String key, V value);

    /**
     * Atomically compute a new value for a key. The remapping function is applied atomically with respect
     * to other mutations of the <em>same key</em> on this near cache, so a read-modify-write (such as
     * applying a merge patch onto the value already held) cannot interleave with a concurrent
     * {@link #put(String, Object)} or another {@code compute} for that key and lose an update.
     *
     * @param key               the key to update.
     * @param remappingFunction given the key and the current value (or {@code null} if absent), returns the
     *                          new value to store, or {@code null} to remove the mapping.
     * @return the new value associated with the key, or {@code null} if the mapping was removed.
     */
    V compute(String key, BiFunction<String, V, V> remappingFunction);

    void remove(String key);

    void clear();

    /**
     * A snapshot of the current key/value pairs held locally.
     *
     * @return a copy of the local contents.
     */
    Map<String, V> snapshot();

    /**
     * The number of items held locally.
     *
     * @return the item count.
     */
    int size();
}
