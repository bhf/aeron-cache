package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;

/**
 * Blocking versions of {@link ClusterMessagePublisher}'s public API.
 */
public interface BlockingClusterRequestPublisher {
    void sendCreateCacheBlocking(AeronCache cluster, String requestId, long cacheId);

    void addCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key, String value);

    void getCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key);

    void clearCacheBlocking(AeronCache cluster, String requestId, long cacheId);

    void deleteCacheBlocking(AeronCache cluster, String requestId, long cacheId);

    void removeCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key);

    void getCacheEntriesBlocking(AeronCache cluster, String requestId, long cacheId);

    void getAllCacheStatsBlocking(AeronCache cluster, String requestId);

    void sendCacheSubscribeBlocking(AeronCache cluster, String requestId, long cacheId);

    void sendCacheUnsubscribeBlocking(AeronCache cluster, String requestId, long cacheId);
}
