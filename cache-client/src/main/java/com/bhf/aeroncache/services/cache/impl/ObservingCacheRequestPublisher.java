package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestConsumingPublisher;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import com.bhf.aeroncache.services.cluster.ClusterRequestConsumingPublisher;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
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
public class ObservingCacheRequestPublisher implements CacheRequestPublisher, CacheRequestConsumingPublisher, CacheResponseHandler {

    private final CacheRequestPublisher rbPublisher;
    private final CacheResponseObservers cacheResponseObservers = new CacheResponseObservers();
    private final CacheResponseCallbackHandler cacheResponseHandler = new CacheResponseCallbackHandler(cacheResponseObservers);


    public ObservingCacheRequestPublisher onCreateCache(Consumer<CreateCacheResult<ReusableLong>> c) {
        cacheResponseObservers.createCacheConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onAddCacheEntry(Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c) {
        cacheResponseObservers.addCacheEntryConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onClearCache(Consumer<ClearCacheResult<ReusableLong>> c) {
        cacheResponseObservers.clearCacheConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onDeleteCache(Consumer<DeleteCacheResult<ReusableLong>> c) {
        cacheResponseObservers.deleteCacheConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onRemoveCacheEntry(Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c) {
        cacheResponseObservers.removeCacheEntryConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onGetCacheEntry(Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c) {
        cacheResponseObservers.getCacheEntryConsumer = c;
        return this;
    }

    @Override
    public void sendCreateCache(String requestId, long cacheId) {
        rbPublisher.sendCreateCache(requestId, cacheId);
    }

    @Override
    public void sendCreateCache(long cacheId, Consumer<CreateCacheResult<ReusableLong>> consumer, String requestId) {
        cacheResponseObservers.sendCreateCache(cacheId, consumer, requestId);
        rbPublisher.sendCreateCache(requestId, cacheId);
    }


    @Override
    public void addCacheEntry(String requestId, long cacheId, String key, String value) {
        rbPublisher.addCacheEntry(requestId, cacheId, key, value);
    }

    @Override
    public void addCacheEntry(long cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c, String requestId) {
        cacheResponseObservers.addCacheEntry(cacheId, key, value, c, requestId);
        rbPublisher.addCacheEntry(requestId, cacheId, key, value);
    }

    @Override
    public void getCacheEntry(String requestId, long cacheId, String key) {
        rbPublisher.getCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void getCacheEntry(long cacheId, String key, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c, String requestId) {
        cacheResponseObservers.getCacheEntry(cacheId, key, c, requestId);
        rbPublisher.getCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void clearCache(String requestId, long cacheId) {
        rbPublisher.clearCache(requestId, cacheId);
    }

    @Override
    public void clearCache(long cacheId, Consumer<ClearCacheResult<ReusableLong>> c, String requestId) {
        cacheResponseObservers.clearCache(cacheId, c, requestId);
        rbPublisher.clearCache(requestId, cacheId);
    }

    @Override
    public void deleteCache(String requestId, long cacheId) {
        rbPublisher.deleteCache(requestId, cacheId);
    }

    @Override
    public void deleteCache(long cacheId, Consumer<DeleteCacheResult<ReusableLong>> consumer, String requestId) {
        cacheResponseObservers.deleteCache(cacheId, consumer, requestId);
        rbPublisher.deleteCache(requestId, cacheId);
    }

    @Override
    public void removeCacheEntry(String requestId, long cacheId, String key) {
        rbPublisher.removeCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void removeCacheEntry(long cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c, String requestId) {
        cacheResponseObservers.removeCacheEntry(cacheId, key, c, requestId);
        rbPublisher.removeCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void getCacheEntries(String requestId, long cacheId) {
        rbPublisher.getCacheEntries(requestId, cacheId);
    }

    @Override
    public void getCacheEntries(long cacheId, Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> c, String requestId) {
        cacheResponseObservers.getCacheEntries(cacheId, c, requestId);
        rbPublisher.getCacheEntries(requestId, cacheId);
    }

    @Override
    public void getAllCacheStats(String requestId) {
        rbPublisher.getAllCacheStats(requestId);
    }

    @Override
    public void getAllCacheStats(Consumer<CacheStatsResult<ReusableLong>> c, String requestId) {
        cacheResponseObservers.getAllCacheStats(c, requestId);
        rbPublisher.getAllCacheStats(requestId);
    }

    @Override
    public void sendCacheSubscribe(String requestId, long cacheId) {
        rbPublisher.sendCacheSubscribe(requestId, cacheId);
    }

    @Override
    public void sendCacheSubscribe(long cacheId, Consumer<CacheSubscriptionResult<ReusableLong>> c, String requestId) {
        cacheResponseObservers.sendCacheSubscribe(cacheId, c, requestId);
        rbPublisher.sendCacheSubscribe(requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, long cacheId) {
        rbPublisher.sendCacheUnsubscribe(requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribe(long cacheId, Consumer<CacheUnsubscribeResult<ReusableLong>> c, String requestId) {
        cacheResponseObservers.sendCacheUnsubscribe(cacheId, c, requestId);
        rbPublisher.sendCacheUnsubscribe(requestId, cacheId);
    }

    /**
     * Handle a message indicating the value of a get operation on a particular key
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntryResult The result of getting something from the cache.
     */
    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult) {
        cacheResponseHandler.handleCacheEntryResult(getCacheEntryResult);
    }

    /**
     * Handle a message indicating the values of an entire cache
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntriesResult The result of getting all items from the cache.
     */
    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult) {
        cacheResponseHandler.handleAllCacheEntries(getCacheEntriesResult);
    }

    /**
     * Handle a message indicating a cache has been created and delegate it
     * to any relevant consumer.
     *
     * @param createCacheResult The result of creating a cache.
     */
    @Override
    public void handleCacheCreated(CreateCacheResult<ReusableLong> createCacheResult) {
        cacheResponseHandler.handleCacheCreated(createCacheResult);
    }

    /**
     * Handle a message indicating a cache entry has been created and delegate it
     * to any relevant consumer.
     *
     * @param addCacheEntryResult The result of adding an entry to the cache.
     */
    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult) {

        cacheResponseHandler.handleCacheEntryCreated(addCacheEntryResult);
    }

    /**
     * Handle a message indicating a cache entry has been removed and delegate it
     * to any relevant consumer.
     *
     * @param removeCacheEntryResult The result of a cache entry removal.
     */
    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
        cacheResponseHandler.handleCacheEntryRemoved(removeCacheEntryResult);
    }

    /**
     * Handle a message indicating a cache has been cleared and delegate it
     * to any relevant consumer.
     *
     * @param clearCacheResult The result of clearing a cache.
     */
    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableLong> clearCacheResult) {
        cacheResponseHandler.handleCacheCleared(clearCacheResult);
    }

    /**
     * Handle a message indicating a cache has been deleted and delegate it
     * to any relevant consumer.
     *
     * @param deleteCacheResult The result of deleting a cache.
     */
    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableLong> deleteCacheResult) {
        cacheResponseHandler.handleCacheDeleted(deleteCacheResult);
    }

    /**
     * Handle a message with all cache stats, delegating it
     * to any relevant consumer.
     *
     * @param statsResult The result of getting all cache stats.
     */
    @Override
    public void handleAllCacheStats(CacheStatsResult<ReusableLong> statsResult) {
        cacheResponseHandler.handleAllCacheStats(statsResult);
    }

    /**
     * Handle a message about a subscription request to a cache.
     *
     * @param cacheSubscriptionResult The result of subscribing to a cache.
     */
    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult) {
        cacheResponseHandler.handleCacheSubscribeResponse(cacheSubscriptionResult);
    }

    /**
     * Handle a message about an unsubscribe request to a cache.
     *
     * @param cacheUnsubscribeResult The result of unsubscribing to a cache.
     */
    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult) {
        cacheResponseHandler.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult) {

        cacheResponseHandler.handleCacheEntryUpdated(cacheEntryUpdateResult);
    }
}
