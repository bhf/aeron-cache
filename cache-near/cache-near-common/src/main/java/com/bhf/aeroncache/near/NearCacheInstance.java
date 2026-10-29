package com.bhf.aeroncache.near;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Log4j2
public class NearCacheInstance<V> implements NearCache<V> {

    private final String cacheName;

    private final ConcurrentHashMap<String, V> localCache = new ConcurrentHashMap<>();

    @Override
    public boolean containsKey(String key) {
        log.debug("Checking near cache '{}' for key '{}'", cacheName, key);
        return localCache.containsKey(key);
    }

    @Override
    public V get(String key) {
        log.debug("Getting from near cache '{}' for key '{}'", cacheName, key);
        return localCache.get(key);
    }

    @Override
    public void put(String key, V value) {
        log.debug("Putting into near cache '{}' key '{}' with value '{}'", cacheName, key, value);
        localCache.put(key, value);
    }

    @Override
    public void remove(String key) {
        log.debug("Removing from near cache '{}' key '{}'", cacheName, key);
        localCache.remove(key);
    }

    @Override
    public void clear() {
        log.debug("Clearing near cache '{}'", cacheName);
        localCache.clear();
    }

    @Override
    public Map<String, V> snapshot() {
        return new LinkedHashMap<>(localCache);
    }

    @Override
    public int size() {
        return localCache.size();
    }
}
