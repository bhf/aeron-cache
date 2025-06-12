package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.services.cluster.BlockingClusterRequestPublisher;
import lombok.extern.log4j.Log4j2;

/**
 * Cluster service which extends {@link ObservingCacheRequestPublisher}
 * to support blocking operations.
 */
@Log4j2
public class ObservingClusterRequestPublisher extends ObservingCacheRequestPublisher implements BlockingClusterRequestPublisher {

    private final BlockingClusterRequestPublisher blockingPublisher;

    public ObservingClusterRequestPublisher(CacheRequestPublisher rbPublisher, BlockingClusterRequestPublisher blockingPublisher) {
        super(rbPublisher);
        this.blockingPublisher = blockingPublisher;
    }

    @Override
    public void sendCreateCacheBlocking(String requestId, long cacheId) {
        blockingPublisher.sendCreateCacheBlocking(requestId, cacheId);
    }

    @Override
    public void addCacheEntryBlocking(String requestId, long cacheId, String key, String value) {
        blockingPublisher.addCacheEntryBlocking(requestId, cacheId, key, value);
    }

    @Override
    public void getCacheEntryBlocking(String requestId, long cacheId, String key) {
        blockingPublisher.getCacheEntryBlocking(requestId, cacheId, key);
    }

    @Override
    public void clearCacheBlocking(String requestId, long cacheId) {
        blockingPublisher.clearCacheBlocking(requestId, cacheId);
    }

    @Override
    public void deleteCacheBlocking(String requestId, long cacheId) {
        blockingPublisher.deleteCacheBlocking(requestId, cacheId);
    }

    @Override
    public void removeCacheEntryBlocking(String requestId, long cacheId, String key) {
        blockingPublisher.removeCacheEntryBlocking(requestId, cacheId, key);
    }

    @Override
    public void getCacheEntriesBlocking(String requestId, long cacheId) {
        blockingPublisher.getCacheEntriesBlocking(requestId, cacheId);
    }

    @Override
    public void getAllCacheStatsBlocking(String requestId) {
        blockingPublisher.getAllCacheStatsBlocking(requestId);
    }

    @Override
    public void sendCacheSubscribeBlocking(String requestId, long cacheId) {
        blockingPublisher.sendCacheSubscribeBlocking(requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribeBlocking(String requestId, long cacheId) {
        blockingPublisher.sendCacheUnsubscribeBlocking(requestId, cacheId);
    }

}
