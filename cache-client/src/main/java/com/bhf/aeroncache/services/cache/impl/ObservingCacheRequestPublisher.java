package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import com.bhf.aeroncache.services.cluster.ClusterRequestConsumingPublisher;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
public class ObservingCacheRequestPublisher implements CacheRequestPublisher, CacheResponseHandler {

    private final CacheRequestPublisher rbPublisher;
    private final List<IdentifiableConsumer<String, CreateCacheResult<ReusableLong>>> createCacheObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, AddCacheEntryResult<ReusableLong, ReusableString>>> addCacheEntryObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>>> getCacheEntryObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, DeleteCacheResult<ReusableLong>>> deleteCacheObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, RemoveCacheEntryResult<ReusableLong, ReusableString>>> removeCacheEntryObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, ClearCacheResult<ReusableLong>>> clearCacheObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>>> getCacheEntriesObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, CacheStatsResult<ReusableLong>>> allCacheStatsObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, CacheSubscriptionResult<ReusableLong>>> cacheSubscribeObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, CacheUnsubscribeResult<ReusableLong>>> cacheUnsubscribeObservers = new CopyOnWriteArrayList<>();


    private Consumer<CreateCacheResult<ReusableLong>> createCacheConsumer;
    private Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> addCacheEntryConsumer;
    private Consumer<ClearCacheResult<ReusableLong>> clearCacheConsumer;
    private Consumer<DeleteCacheResult<ReusableLong>> deleteCacheConsumer;
    private Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> removeCacheEntryConsumer;
    private Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> getCacheEntryConsumer;
    private Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> getCacheEntriesConsumer;

    public ObservingCacheRequestPublisher onCreateCache(Consumer<CreateCacheResult<ReusableLong>> c) {
        createCacheConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onAddCacheEntry(Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c) {
        addCacheEntryConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onClearCache(Consumer<ClearCacheResult<ReusableLong>> c) {
        clearCacheConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onDeleteCache(Consumer<DeleteCacheResult<ReusableLong>> c) {
        deleteCacheConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onRemoveCacheEntry(Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c) {
        removeCacheEntryConsumer = c;
        return this;
    }

    public ObservingCacheRequestPublisher onGetCacheEntry(Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c) {
        getCacheEntryConsumer = c;
        return this;
    }

    @Override
    public void sendCreateCache(String requestId, long cacheId) {
        rbPublisher.sendCreateCache(requestId, cacheId);
    }


    @Override
    public void addCacheEntry(String requestId, long cacheId, String key, String value) {
        rbPublisher.addCacheEntry(requestId, cacheId, key, value);
    }

    @Override
    public void getCacheEntry(String requestId, long cacheId, String key) {
        rbPublisher.getCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void clearCache(String requestId, long cacheId) {
        rbPublisher.clearCache(requestId, cacheId);
    }

    @Override
    public void deleteCache(String requestId, long cacheId) {
        rbPublisher.deleteCache(requestId, cacheId);
    }

    @Override
    public void removeCacheEntry(String requestId, long cacheId, String key) {
        rbPublisher.removeCacheEntry(requestId, cacheId, key);
    }

    @Override
    public void getCacheEntries(String requestId, long cacheId) {
        rbPublisher.getCacheEntries(requestId, cacheId);
    }

    @Override
    public void getAllCacheStats(String requestId) {
        rbPublisher.getAllCacheStats(requestId);
    }

    @Override
    public void sendCacheSubscribe(String requestId, long cacheId) {
        rbPublisher.sendCacheSubscribe(requestId, cacheId);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, long cacheId) {
        rbPublisher.sendCacheUnsubscribe(requestId, cacheId);
    }

    /**
     * Handle a message indicating the value of a get operation on a particular key
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntryResult The result of getting something from the cache.
     */
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult) {
        var targetId = getCacheEntryResult.getRequestId();
        getCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntryResult));
        getCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (getCacheEntryConsumer != null) {
            getCacheEntryConsumer.accept(getCacheEntryResult);
        }
    }

    /**
     * Handle a message indicating the values of an entire cache
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntriesResult The result of getting all items from the cache.
     */
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult) {
        var targetId = getCacheEntriesResult.getRequestId();
        getCacheEntriesObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntriesResult));
        getCacheEntriesObservers.removeIf(p -> p.getId().equals(targetId));
        if (getCacheEntriesConsumer != null) {
            getCacheEntriesConsumer.accept(getCacheEntriesResult);
        }
    }

    /**
     * Handle a message indicating a cache has been created and delegate it
     * to any relevant consumer.
     *
     * @param createCacheResult The result of creating a cache.
     */
    public void handleCacheCreated(CreateCacheResult<ReusableLong> createCacheResult) {
        var targetId = createCacheResult.getRequestId();
        createCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(createCacheResult));
        createCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (createCacheConsumer != null) {
            createCacheConsumer.accept(createCacheResult);
        }
    }

    /**
     * Handle a message indicating a cache entry has been created and delegate it
     * to any relevant consumer.
     *
     * @param addCacheEntryResult The result of adding an entry to the cache.
     */
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult) {
        var targetId = addCacheEntryResult.getRequestId();
        addCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(addCacheEntryResult));
        addCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (addCacheEntryConsumer != null) {
            addCacheEntryConsumer.accept(addCacheEntryResult);
        }

    }

    /**
     * Handle a message indicating a cache entry has been removed and delegate it
     * to any relevant consumer.
     *
     * @param removeCacheEntryResult The result of a cache entry removal.
     */
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
        var targetId = removeCacheEntryResult.getRequestId();
        removeCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(removeCacheEntryResult));
        removeCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (removeCacheEntryConsumer != null) {
            removeCacheEntryConsumer.accept(removeCacheEntryResult);
        }
    }

    /**
     * Handle a message indicating a cache has been cleared and delegate it
     * to any relevant consumer.
     *
     * @param clearCacheResult The result of clearing a cache.
     */
    public void handleCacheCleared(ClearCacheResult<ReusableLong> clearCacheResult) {
        var targetId = clearCacheResult.getRequestId();
        clearCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(clearCacheResult));
        clearCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (clearCacheConsumer != null) {
            clearCacheConsumer.accept(clearCacheResult);
        }
    }

    /**
     * Handle a message indicating a cache has been deleted and delegate it
     * to any relevant consumer.
     *
     * @param deleteCacheResult The result of deleting a cache.
     */
    public void handleCacheDeleted(DeleteCacheResult<ReusableLong> deleteCacheResult) {
        var targetId = deleteCacheResult.getRequestId();
        deleteCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(deleteCacheResult));
        deleteCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (deleteCacheConsumer != null) {
            deleteCacheConsumer.accept(deleteCacheResult);
        }
    }

    /**
     * Handle a message with all cache stats, delegating it
     * to any relevant consumer.
     *
     * @param statsResult The result of getting all cache stats.
     */
    public void handleAllCacheStats(CacheStatsResult<ReusableLong> statsResult) {
        var targetId = statsResult.getRequestId();
        allCacheStatsObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(statsResult));
        allCacheStatsObservers.removeIf(p -> p.getId().equals(targetId));
    }

    /**
     * Handle a message about a subscription request to a cache.
     *
     * @param cacheSubscriptionResult The result of subscribing to a cache.
     */
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult) {
        var targetId = cacheSubscriptionResult.getRequestId();
        log.info("Got cache subscribe response on requestId {}", targetId);
        cacheSubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheSubscriptionResult));
        cacheSubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    /**
     * Handle a message about an unsubscribe request to a cache.
     *
     * @param cacheUnsubscribeResult The result of unsubscribing to a cache.
     */
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult) {
        var targetId = cacheUnsubscribeResult.getRequestId();
        log.info("Got cache unsubscribe response on requestId {}", targetId);
        cacheUnsubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheUnsubscribeResult));
        cacheUnsubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult) {

    }
}
