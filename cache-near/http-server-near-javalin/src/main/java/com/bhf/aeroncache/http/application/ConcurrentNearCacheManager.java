package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class ConcurrentNearCacheManager {

    private final ConcurrentHashMap<String, NearCacheInstance> nearCacheMap = new ConcurrentHashMap<>();

    public void put(String cacheId, String key, String value) {
        nearCacheMap.computeIfAbsent(cacheId, NearCacheInstance::new).put(key, value);
    }

    public boolean contains(String cacheId) {
        return nearCacheMap.containsKey(cacheId);
    }

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

    public NearCacheInstance get(String cacheId) {
        return nearCacheMap.get(cacheId);
    }
}
