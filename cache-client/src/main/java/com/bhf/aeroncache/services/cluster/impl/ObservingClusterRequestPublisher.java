package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.services.cluster.BlockingClusterRequestPublisher;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 * Cluster service which extends {@link ObservingCacheRequestPublisher}
 * to support blocking operations.
 */
@Log4j2
public class ObservingClusterRequestPublisher<I extends Reusable, K extends Reusable, V extends Reusable, BI, BK, BV>
        extends ObservingCacheRequestPublisher<I,K,V,BI,BK,BV> implements BlockingClusterRequestPublisher<BI, BK, BV> {

    private final BlockingClusterRequestPublisher<BI,BK,BV> blockingPublisher;

    public ObservingClusterRequestPublisher(CacheRequestPublisher<BI,BK,BV> rbPublisher, BlockingClusterRequestPublisher<BI,BK,BV> blockingPublisher) {
        super(rbPublisher);
        this.blockingPublisher = blockingPublisher;
    }

    @Override
    public void sendCreateCacheBlocking(String requestId, BI cacheId) {
        blockingPublisher.sendCreateCacheBlocking(requestId, cacheId);
    }

    @Override
    public void addCacheEntryBlocking(String requestId, BI cacheId, BK key, BV value, long ttl) {
        blockingPublisher.addCacheEntryBlocking(requestId, cacheId, key, value, ttl);
    }

    @Override
    public void getCacheEntryBlocking(String requestId, BI cacheId, BK key) {
        blockingPublisher.getCacheEntryBlocking(requestId, cacheId, key);
    }

    @Override
    public void clearCacheBlocking(String requestId, BI cacheId) {
        blockingPublisher.clearCacheBlocking(requestId, cacheId);
    }

    @Override
    public void deleteCacheBlocking(String requestId, BI cacheId) {
        blockingPublisher.deleteCacheBlocking(requestId, cacheId);
    }

    @Override
    public void removeCacheEntryBlocking(String requestId, BI cacheId, BK key) {
        blockingPublisher.removeCacheEntryBlocking(requestId, cacheId, key);
    }

    @Override
    public void cancelItemRemovalBlocking(String requestId, BI cacheId, BK key) {
        blockingPublisher.cancelItemRemovalBlocking(requestId, cacheId, key);
    }

    @Override
    public void getCacheEntriesBlocking(String requestId, BI cacheId) {
        blockingPublisher.getCacheEntriesBlocking(requestId, cacheId);
    }

    @Override
    public void sendBulkOperationsBlocking(String requestId, BulkCacheOpsRequest request) {
        blockingPublisher.sendBulkOperationsBlocking(requestId, request);
    }

    @Override
    public void getAllCacheStatsBlocking(String requestId) {
        blockingPublisher.getAllCacheStatsBlocking(requestId);
    }

    @Override
    public void sendCacheSubscribeBlocking(String requestId, List<BI> cacheId, boolean sendSnapshot) {
        blockingPublisher.sendCacheSubscribeBlocking(requestId, cacheId, sendSnapshot);
    }

    @Override
    public void sendCacheUnsubscribeBlocking(String requestId, BI cacheId) {
        blockingPublisher.sendCacheUnsubscribeBlocking(requestId, cacheId);
    }

}
