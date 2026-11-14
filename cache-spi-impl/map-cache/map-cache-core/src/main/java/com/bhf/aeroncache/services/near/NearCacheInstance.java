package com.bhf.aeroncache.services.near;

import com.bhf.aeroncache.near.NearCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@RequiredArgsConstructor
@Log4j2
public class NearCacheInstance<K, V> implements NearCache<K, V> {

    private final String cacheName;

    private final ConcurrentHashMap<K, V> localCache = new ConcurrentHashMap<>();

    @Override
    public boolean containsKey(K key) {
        log.debug("Checking near cache '{}' for key '{}'", cacheName, key);
        return localCache.containsKey(key);
    }

    @Override
    public V get(K key) {
        log.debug("Getting from near cache '{}' for key '{}'", cacheName, key);
        return localCache.get(key);
    }

    @Override
    public void put(K key, V value) {
        log.debug("Putting into near cache '{}' key '{}' with value '{}'", cacheName, key, value);
        localCache.put(key, value);
    }

    @Override
    public V compute(K key, BiFunction<K, V, V> remappingFunction) {
        log.debug("Computing new value in near cache '{}' for key '{}'", cacheName, key);
        return localCache.compute(key, remappingFunction);
    }

    @Override
    public void remove(K key) {
        log.debug("Removing from near cache '{}' key '{}'", cacheName, key);
        localCache.remove(key);
    }

    @Override
    public void clear() {
        log.debug("Clearing near cache '{}'", cacheName);
        localCache.clear();
    }

    @Override
    public Map<K, V> snapshot() {
        return new LinkedHashMap<>(localCache);
    }

    @Override
    public int size() {
        return localCache.size();
    }
}
