package com.bhf.aeroncache.near;

import com.bhf.aeroncache.http.responses.CacheItem;

import java.util.List;

/**
 * A single named near (local) cache holding string key/value pairs.
 */
public interface NearCache {

    List<CacheItem> items();

    boolean containsKey(String key);

    String get(String key);

    void put(String key, String value);

    void remove(String key);

    void clear();
}
