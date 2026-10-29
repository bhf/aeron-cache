package com.bhf.aeroncache.near;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;

import java.util.Set;

/**
 * Manages a set of named {@link NearCache}s holding values of type {@code V}, keeping them in sync
 * with cache update events.
 */
public interface NearCacheManager<V> {

    void put(String cacheId, String key, V value);

    boolean contains(String cacheId);

    NearCache<V> get(String cacheId);

    void handleCacheUpdate(CacheUpdateEvent<V> cacheUpdateEvent);

    /**
     * The IDs of every cache currently held in the near cache.
     *
     * @return a snapshot of the warm cache IDs.
     */
    Set<String> cacheIds();

    /**
     * Drop the local copy of a cache, if present.
     *
     * @param cacheId the cache to remove.
     * @return true if a local copy existed and was removed.
     */
    boolean remove(String cacheId);
}
