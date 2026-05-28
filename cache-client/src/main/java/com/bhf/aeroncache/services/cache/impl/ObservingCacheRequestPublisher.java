package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestConsumingPublisher;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import com.bhf.aeroncache.services.cache.ConsumingResponseHandler;
import com.bhf.aeroncache.types.ReusableString;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.function.Consumer;

/**
 * A simple observer that delegates methods which don't pass in a {@link Consumer}.
 * <p>
 */
@RequiredArgsConstructor
@Log4j2
public class ObservingCacheRequestPublisher<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheRequestPublisher, CacheRequestConsumingPublisher<I,K,V>, CacheResponseHandler<I,K,V> {

    private final CacheRequestPublisher rbPublisher;
    private final ConsumingResponseHandler cacheResponseObservers = new CacheResponseMapObservers();
    private final CacheResponseCallbackHandler cacheResponseHandler = new CacheResponseCallbackHandler(cacheResponseObservers);

    public ObservingCacheRequestPublisher onCreateCache(Consumer<CreateCacheResult<ReusableString>> c) {
        cacheResponseObservers.setCreateCacheConsumer(c);
        return this;
    }

    public ObservingCacheRequestPublisher onAddCacheEntry(Consumer<AddCacheEntryResult<ReusableString, ReusableString>> c) {
        cacheResponseObservers.setAddCacheEntryConsumer(c);
        return this;
    }

    public ObservingCacheRequestPublisher onClearCache(Consumer<ClearCacheResult<ReusableString>> c) {
        cacheResponseObservers.setClearCacheConsumer(c);
        return this;
    }

    public ObservingCacheRequestPublisher onDeleteCache(Consumer<DeleteCacheResult<ReusableString>> c) {
        cacheResponseObservers.setDeleteCacheConsumer(c);
        return this;
    }

    public ObservingCacheRequestPublisher onRemoveCacheEntry(Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> c) {
        cacheResponseObservers.setRemoveCacheEntryConsumer(c);
        return this;
    }

    public ObservingCacheRequestPublisher onGetCacheEntry(Consumer<GetCacheEntryResult<ReusableString, ReusableString, ReusableString>> c) {
        cacheResponseObservers.setGetCacheEntryConsumer(c);
        return this;
    }

    @Override
    public void sendCreateCache(String requestId, String cacheId) {
        rbPublisher.sendCreateCache(requestId, cacheId);
    }

    @Override
    public void sendCreateCache(String requestId, String cacheId, Consumer<CreateCacheResult<I>> consumer) {
        cacheResponseObservers.sendCreateCache(requestId, cacheId, consumer);
        rbPublisher.sendCreateCache(requestId, cacheId);
    }


    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, long ttl) {
        rbPublisher.addCacheEntry(requestId, cacheId, key, value, ttl);
    }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, long ttl, Consumer<AddCacheEntryResult<I, K>> c) {
        cacheResponseObservers.addCacheEntry(requestId, cacheId, key, value, ttl, c);
        rbPublisher.addCacheEntry(requestId, cacheId, key, value, ttl);
    }

    @Override
    public void getCacheEntry(String requestId, String cacheId, String key) {
        rbPublisher.getCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void getCacheEntry(String requestId, String cacheId, String key, Consumer<GetCacheEntryResult<I, K, V>> c) {
        cacheResponseObservers.getCacheEntry(requestId, cacheId, key, c);
        rbPublisher.getCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void clearCache(String requestId, String cacheId) {
        rbPublisher.clearCache(requestId, cacheId);
    }

    @Override
    public void clearCache(String requestId, String cacheId, Consumer<ClearCacheResult<I>> c) {
        cacheResponseObservers.clearCache(requestId, cacheId, c);
        rbPublisher.clearCache(requestId, cacheId);
    }

    @Override
    public void deleteCache(String requestId, String cacheId) {
        rbPublisher.deleteCache(requestId, cacheId);
    }

    @Override
    public void deleteCache(String requestId, String cacheId, Consumer<DeleteCacheResult<I>> consumer) {
        cacheResponseObservers.deleteCache(requestId, cacheId, consumer);
        rbPublisher.deleteCache(requestId, cacheId);
    }

    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key) {
        rbPublisher.removeCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key, Consumer<RemoveCacheEntryResult<I, K>> c) {
        cacheResponseObservers.removeCacheEntry(requestId, cacheId, key, c);
        rbPublisher.removeCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId) {
        rbPublisher.getCacheEntries(requestId, cacheId);
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId, Consumer<GetAllCacheEntriesResult<I, K, V>> c) {
        cacheResponseObservers.getCacheEntries(requestId, cacheId, c);
        rbPublisher.getCacheEntries(requestId, cacheId);
    }

    @Override
    public void getAllCacheStats(String requestId) {
        rbPublisher.getAllCacheStats(requestId);
    }

    @Override
    public void getAllCacheStats(String requestId, Consumer<CacheStatsResult<I>> c) {
        cacheResponseObservers.getAllCacheStats(requestId, c);
        rbPublisher.getAllCacheStats(requestId);
    }

    @Override
    public void sendCacheSubscribe(String requestId, String cacheId) {
        rbPublisher.sendCacheSubscribe(requestId, cacheId);
    }

    @Override
    public void sendCacheSubscribe(String requestId, String cacheId, Consumer<CacheSubscriptionResult<I>> c) {
        cacheResponseObservers.sendCacheSubscribe(requestId, cacheId, c);
        rbPublisher.sendCacheSubscribe(requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId) {
        rbPublisher.sendCacheUnsubscribe(requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId, Consumer<CacheUnsubscribeResult<I>> c) {
        cacheResponseObservers.sendCacheUnsubscribe(requestId, cacheId, c);
        rbPublisher.sendCacheUnsubscribe(requestId, cacheId);
    }

    @Override
    public void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request) {
        rbPublisher.sendBulkOperationsRequest(requestId, request);
    }

    @Override
    public void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request, Consumer<BulkCacheOpsResult<I,K,V>> consumer) {
        cacheResponseObservers.sendBulkOperationsRequest(requestId, request, consumer);
        rbPublisher.sendBulkOperationsRequest(requestId, request);
    }

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<I, K, V> getCacheEntryResult) {
        cacheResponseHandler.handleCacheEntryResult(getCacheEntryResult);
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult) {
        cacheResponseHandler.handleAllCacheEntries(getCacheEntriesResult);
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<I> createCacheResult) {
        cacheResponseHandler.handleCacheCreated(createCacheResult);
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<I, K> addCacheEntryResult) {
        cacheResponseHandler.handleCacheEntryCreated(addCacheEntryResult);
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
        cacheResponseHandler.handleCacheEntryRemoved(removeCacheEntryResult);
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
        cacheResponseHandler.handleCacheCleared(clearCacheResult);
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
        cacheResponseHandler.handleCacheDeleted(deleteCacheResult);
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<I> statsResult) {
        cacheResponseHandler.handleAllCacheStats(statsResult);
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<I> cacheSubscriptionResult) {
        cacheResponseHandler.handleCacheSubscribeResponse(cacheSubscriptionResult);
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<I> cacheUnsubscribeResult) {
        cacheResponseHandler.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {
        cacheResponseHandler.handleCacheEntryUpdated(cacheEntryUpdateResult);
    }

    @Override
    public void handleBulkOperationsResult(BulkCacheOpsResult<I, K, V> bulkCacheOpsResult) {
        cacheResponseHandler.handleBulkOperationsResult(bulkCacheOpsResult);
    }
}
