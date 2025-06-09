package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import com.bhf.aeroncache.services.cache.impl.CacheResponseCallbackHandler;
import com.bhf.aeroncache.services.cache.impl.CacheResponseObservers;
import com.bhf.aeroncache.services.cluster.ClusterRequestConsumingPublisher;
import com.bhf.aeroncache.services.cluster.ClusterRequestPublisher;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.function.Consumer;

/**
 * A simple observer that delegates methods which don't pass in a {@link Consumer} directly
 * to the {@link ClusterMessagePublisher}.
 * <p>
 * Methods which accept a {@link Consumer} and are implementations of the {@link ClusterRequestConsumingPublisher}
 * use a CoW observer style approach after wrapping the consumer into an
 * {@link IdentifiableConsumer} with an internally generated Id.
 */
@RequiredArgsConstructor
@Log4j2
public class ObservingClusterRequestPublisher implements ClusterRequestPublisher, ClusterRequestConsumingPublisher, CacheResponseHandler, BlockingClusterRequestPublisher {

    private final ClusterMessagePublisher publisher;
    private final CacheResponseObservers cacheResponseObservers = new CacheResponseObservers();
    private final CacheResponseCallbackHandler callbackHandler = new CacheResponseCallbackHandler(cacheResponseObservers);

    public ObservingClusterRequestPublisher onCreateCache(Consumer<CreateCacheResult<ReusableLong>> c) {
        cacheResponseObservers.setCreateCacheConsumer(c);
        return this;
    }

    public ObservingClusterRequestPublisher onAddCacheEntry(Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c) {
        cacheResponseObservers.setAddCacheEntryConsumer(c);
        return this;
    }

    public ObservingClusterRequestPublisher onClearCache(Consumer<ClearCacheResult<ReusableLong>> c) {
        cacheResponseObservers.setClearCacheConsumer(c);
        return this;
    }

    public ObservingClusterRequestPublisher onDeleteCache(Consumer<DeleteCacheResult<ReusableLong>> c) {
        cacheResponseObservers.setDeleteCacheConsumer(c);
        return this;
    }

    public ObservingClusterRequestPublisher onRemoveCacheEntry(Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c) {
        cacheResponseObservers.setRemoveCacheEntryConsumer(c);
        return this;
    }

    public ObservingClusterRequestPublisher onGetCacheEntry(Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c) {
        cacheResponseObservers.setGetCacheEntryConsumer(c);
        return this;
    }

    @Override
    public void sendCreateCache(AeronCache cluster, String requestId, long cacheId) {
        publisher.sendCreateCache(cluster, requestId, cacheId);
    }

    @Override
    public void sendCreateCacheBlocking(AeronCache cluster, String requestId, long cacheId) {
        publisher.sendCreateCacheBlocking(cluster, requestId, cacheId);
    }

    @Override
    public void sendCreateCacheBlocking(AeronCache cluster, long cacheId, Consumer<CreateCacheResult<ReusableLong>> consumer, String requestId) {
        cacheResponseObservers.sendCreateCache(cacheId, consumer, requestId);
        publisher.sendCreateCache(cluster, requestId, cacheId);
    }

    @Override
    public void addCacheEntry(AeronCache cluster, String requestId, long cacheId, String key, String value) {
        publisher.addCacheEntry(cluster, requestId, cacheId, key, value);
    }

    @Override
    public void addCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key, String value) {
        publisher.addCacheEntryBlocking(cluster, requestId, cacheId, key, value);
    }

    @Override
    public void addCacheEntryBlocking(AeronCache cluster, long cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c, String requestId) {
        cacheResponseObservers.addCacheEntry(cacheId, key, value, c, requestId);
        publisher.addCacheEntry(cluster, requestId, cacheId, key, value);
    }

    @Override
    public void getCacheEntry(AeronCache cluster, String requestId, long cacheId, String key) {
        publisher.getCacheEntry(cluster, requestId, cacheId, key);
    }

    @Override
    public void getCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key) {
        publisher.getCacheEntryBlocking(cluster, requestId, cacheId, key);
    }

    @Override
    public void getCacheEntryBlocking(AeronCache cluster, long cacheId, String key, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c, String requestId) {
        cacheResponseObservers.getCacheEntry(cacheId, key, c, requestId);
        publisher.getCacheEntry(cluster, requestId, cacheId, key);
    }

    @Override
    public void clearCache(AeronCache cluster, String requestId, long cacheId) {
        publisher.clearCache(cluster, requestId, cacheId);
    }

    @Override
    public void clearCacheBlocking(AeronCache cluster, String requestId, long cacheId) {
        publisher.clearCacheBlocking(cluster, requestId, cacheId);
    }

    @Override
    public void clearCacheBlocking(AeronCache cluster, long cacheId, Consumer<ClearCacheResult<ReusableLong>> c, String requestId) {
        cacheResponseObservers.clearCache(cacheId, c, requestId);
        publisher.clearCache(cluster, requestId, cacheId);
    }

    @Override
    public void deleteCache(AeronCache cluster, String requestId, long cacheId) {
        publisher.deleteCache(cluster, requestId, cacheId);
    }

    @Override
    public void deleteCacheBlocking(AeronCache cluster, String requestId, long cacheId) {
        publisher.deleteCacheBlocking(cluster, requestId, cacheId);
    }

    @Override
    public void deleteCacheBlocking(AeronCache cluster, long cacheId, Consumer<DeleteCacheResult<ReusableLong>> consumer, String requestId) {
        cacheResponseObservers.deleteCache(cacheId, consumer, requestId);
        publisher.deleteCache(cluster, requestId, cacheId);
    }

    @Override
    public void removeCacheEntry(AeronCache cluster, String requestId, long cacheId, String key) {
        publisher.removeCacheEntry(cluster, requestId, cacheId, key);
    }

    @Override
    public void removeCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key) {
        publisher.removeCacheEntryBlocking(cluster, requestId, cacheId, key);
    }

    @Override
    public void removeCacheEntryBlocking(AeronCache cluster, long cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c, String requestId) {
        cacheResponseObservers.removeCacheEntry(cacheId, key, c, requestId);
        publisher.removeCacheEntry(cluster, requestId, cacheId, key);
    }

    @Override
    public void getCacheEntries(AeronCache cluster, String requestId, long cacheId) {
        publisher.getCacheEntries(cluster, requestId, cacheId);
    }

    @Override
    public void getCacheEntriesBlocking(AeronCache cluster, String requestId, long cacheId) {
        publisher.getCacheEntriesBlocking(cluster, requestId, cacheId);
    }

    @Override
    public void getCacheEntriesBlocking(AeronCache cluster, long cacheId, Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> c, String requestId) {
        cacheResponseObservers.getCacheEntries(cacheId, c, requestId);
        publisher.getCacheEntries(cluster, requestId, cacheId);
    }

    @Override
    public void getAllCacheStats(AeronCache cluster, String requestId) {
        publisher.getAllCacheStats(cluster, requestId);
    }

    @Override
    public void getAllCacheStatsBlocking(AeronCache cluster, String requestId) {
        publisher.getAllCacheStatsBlocking(cluster, requestId);
    }

    @Override
    public void getAllCacheStatsBlocking(AeronCache cluster, Consumer<CacheStatsResult<ReusableLong>> c, String requestId) {
        cacheResponseObservers.getAllCacheStats(c, requestId);
        publisher.getAllCacheStats(cluster, requestId);
    }

    @Override
    public void sendCacheSubscribe(AeronCache cluster, String requestId, long cacheId) {
        publisher.sendCacheSubscribe(cluster, requestId, cacheId);
    }

    @Override
    public void sendCacheSubscribeBlocking(AeronCache cluster, String requestId, long cacheId) {
        publisher.sendCacheSubscribe(cluster, requestId, cacheId);
    }

    @Override
    public void sendCacheSubscribeBlocking(AeronCache cluster, long cacheId, Consumer<CacheSubscriptionResult<ReusableLong>> c, String requestId) {
        cacheResponseObservers.sendCacheSubscribe(cacheId, c, requestId);
        publisher.sendCacheSubscribe(cluster, requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribe(AeronCache cluster, String requestId, long cacheId) {
        publisher.sendCacheUnsubscribe(cluster, requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribeBlocking(AeronCache cluster, String requestId, long cacheId) {
        publisher.sendCacheUnsubscribe(cluster, requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribeBlocking(AeronCache cluster, long cacheId, Consumer<CacheUnsubscribeResult<ReusableLong>> c, String requestId) {
        cacheResponseObservers.sendCacheUnsubscribe(cacheId, c, requestId);
        publisher.sendCacheUnsubscribe(cluster, requestId, cacheId);
    }


    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult) {
        callbackHandler.handleCacheEntryResult(getCacheEntryResult);
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult) {
        callbackHandler.handleAllCacheEntries(getCacheEntriesResult);
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<ReusableLong> createCacheResult) {
        callbackHandler.handleCacheCreated(createCacheResult);
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult) {
        callbackHandler.handleCacheEntryCreated(addCacheEntryResult);
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
        callbackHandler.handleCacheEntryRemoved(removeCacheEntryResult);
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableLong> clearCacheResult) {
        callbackHandler.handleCacheCleared(clearCacheResult);
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableLong> deleteCacheResult) {
        callbackHandler.handleCacheDeleted(deleteCacheResult);
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<ReusableLong> statsResult) {
        callbackHandler.handleAllCacheStats(statsResult);
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult) {
        callbackHandler.handleCacheSubscribeResponse(cacheSubscriptionResult);
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult) {
        callbackHandler.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult) {
        callbackHandler.handleCacheEntryUpdated(cacheEntryUpdateResult);
    }
}
