package com.bhf.aeroncache.near;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.near.patch.JsonMergePatch;
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
            case PATCH_ITEM -> applyPatch(cacheId, itemKey, itemValue);
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

    /**
     * Apply a JSON merge patch delta onto the value already held locally, reconstructing the full value. A
     * downstream consumer that subscribed for patch updates receives a full {@code ADD_ITEM} baseline first
     * and {@code PATCH_ITEM} deltas thereafter, so by the time a patch arrives the key normally already holds
     * a value. If there is no prior JSON value to merge onto (no baseline yet, or a non-JSON value such as a
     * counter), the patch is treated as the whole value.
     *
     * @param cacheId the cache the update applies to.
     * @param itemKey the item key.
     * @param patch   the merge patch document (its value type carries a JSON string for regular caches).
     */
    @SuppressWarnings("unchecked")
    private void applyPatch(String cacheId, String itemKey, V patch) {
        var instance = nearCacheMap.computeIfAbsent(cacheId, NearCacheInstance::new);
        V existing = instance.get(itemKey);
        if (existing instanceof String existingJson && patch instanceof String patchJson) {
            instance.put(itemKey, (V) JsonMergePatch.applyMergePatch(existingJson, patchJson));
        } else {
            instance.put(itemKey, patch);
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
