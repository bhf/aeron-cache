package com.bhf.aeroncache.near;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import lombok.extern.log4j.Log4j2;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Log4j2
public class ConcurrentNearCacheManager<V> implements NearCacheManager<V> {

    private final ConcurrentMap<String, NearCacheInstance<V>> nearCacheMap = new ConcurrentHashMap<>();

    @Override
    public void put(String cacheId, String key, V value) {
        nearCacheMap.computeIfAbsent(cacheId, NearCacheInstance::new).put(key, value);
    }

    @Override
    public boolean contains(String cacheId) {
        return nearCacheMap.containsKey(cacheId);
    }

    @Override
    public void handleCacheUpdate(CacheUpdateEvent<V> cacheUpdateEvent) {
        handleCacheUpdate(cacheUpdateEvent.cacheId(), cacheUpdateEvent.eventType(),
                cacheUpdateEvent.itemKey(), cacheUpdateEvent.itemValue());
    }

    @Override
    public void handleCacheUpdate(String cacheId, CacheUpdateEvent.EventType eventType, String itemKey, V itemValue) {

        switch(eventType){
            case ADD_ITEM -> put(cacheId, itemKey, itemValue);
            case DELETE_CACHE -> nearCacheMap.remove(cacheId);
            case REMOVE_ITEM -> {
                if(nearCacheMap.containsKey(cacheId)){
                    nearCacheMap.get(cacheId).remove(itemKey);
                }
            }
            case CLEAR_CACHE -> {
                if(nearCacheMap.containsKey(cacheId)){
                    nearCacheMap.get(cacheId).clear();
                }
            }
            default -> log.warn("Unknown update action: {} for cacheId {} key {}", eventType, cacheId, itemKey);
        }
    }

    @Override
    public NearCache<V> get(String cacheId) {
        return nearCacheMap.get(cacheId);
    }

    @Override
    public Set<String> cacheIds() {
        return Set.copyOf(nearCacheMap.keySet());
    }

    @Override
    public boolean remove(String cacheId) {
        return nearCacheMap.remove(cacheId) != null;
    }
}
