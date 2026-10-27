package com.bhf.aeroncache.near;

import com.bhf.aeroncache.http.responses.CacheUpdateEvent;

/**
 * Manages a set of named {@link NearCache}s, keeping them in sync with cache update events.
 */
public interface NearCacheManager {

    void put(String cacheId, String key, String value);

    boolean contains(String cacheId);

    NearCache get(String cacheId);

    void handleCacheUpdate(CacheUpdateEvent cacheUpdateEvent);
}
