package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.http.responses.CacheItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Log4j2
public class NearCacheInstance {

    private final String cacheName;

    private final ConcurrentHashMap<String, String> localCache = new ConcurrentHashMap<>();

    public List<CacheItem> items() {
        return localCache.entrySet().stream()
                .map(e -> new CacheItem(e.getKey(), e.getValue()))
                .toList();
    }

    public boolean containsKey(String key) {
        log.debug("Checking near cache '{}' for key '{}'", cacheName, key);
        return localCache.containsKey(key);
    }

    public String get(String key) {
        log.debug("Getting from near cache '{}' for key '{}'", cacheName, key);
        return localCache.get(key);
    }

    public void put(String key, String value) {
        log.debug("Putting into near cache '{}' key '{}' with value '{}'", cacheName, key, value);
        localCache.put(key, value);
    }

    public void remove(String key) {
        log.debug("Removing from near cache '{}' key '{}'", cacheName, key);
        localCache.remove(key);
    }

    public void clear() {
        log.debug("Clearing near cache '{}'", cacheName);
        localCache.clear();
    }
}
