package com.bhf.aeroncache.near;

import java.util.Map;

/**
 * A single named near (local) cache holding string keys mapped to values of type {@code V}
 * (for example {@code String} for regular caches, {@code Long} for counter caches).
 */
public interface NearCache<V> {

    V get(String key);

    boolean containsKey(String key);

    void put(String key, V value);

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
