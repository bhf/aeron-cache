package com.bhf.aeroncache.services.cache;

/**
 * Requests that an Aeron Cache will handle.
 */
public interface CacheRequestPublisher {
    void sendCreateCache(String requestId, long cacheId);

    void addCacheEntry(String requestId, long cacheId, String key, String value);

    void getCacheEntry(String requestId, long cacheId, String key);

    void clearCache(String requestId, long cacheId);

    void deleteCache(String requestId, long cacheId);

    void removeCacheEntry(String requestId, long cacheId, String key);

    void getCacheEntries(String requestId, long cacheId);

    void getAllCacheStats(String requestId);

    void sendCacheSubscribe(String requestId, long cacheId);

    void sendCacheUnsubscribe(String requestId, long cacheId);
}
