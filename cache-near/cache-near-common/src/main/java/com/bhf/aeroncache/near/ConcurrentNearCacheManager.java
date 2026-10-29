package com.bhf.aeroncache.near;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import lombok.extern.log4j.Log4j2;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Log4j2
public class ConcurrentNearCacheManager implements NearCacheManager {

    private final ConcurrentMap<String, NearCacheInstance> nearCacheMap = new ConcurrentHashMap<>();

    @Override
    public void put(String cacheId, String key, String value) {
        nearCacheMap.computeIfAbsent(cacheId, NearCacheInstance::new).put(key, value);
    }

    @Override
    public boolean contains(String cacheId) {
        return nearCacheMap.containsKey(cacheId);
    }

    @Override
    public void handleCacheUpdate(CacheUpdateEvent cacheUpdateEvent) {

        switch(cacheUpdateEvent.eventType()){
            case ADD_ITEM -> put(cacheUpdateEvent.cacheId(), cacheUpdateEvent.itemKey(), (String) cacheUpdateEvent.itemValue());
            case DELETE_CACHE -> nearCacheMap.remove(cacheUpdateEvent.cacheId());
            case REMOVE_ITEM -> {
                if(nearCacheMap.containsKey(cacheUpdateEvent.cacheId())){
                    nearCacheMap.get(cacheUpdateEvent.cacheId()).remove(cacheUpdateEvent.itemKey());
                }
            }
            case CLEAR_CACHE -> {
                if(nearCacheMap.containsKey(cacheUpdateEvent.cacheId())){
                    nearCacheMap.get(cacheUpdateEvent.cacheId()).clear();
                }
            }
            default -> log.warn("Unknown update action: {} ", cacheUpdateEvent);
        }
    }

    @Override
    public NearCache get(String cacheId) {
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
