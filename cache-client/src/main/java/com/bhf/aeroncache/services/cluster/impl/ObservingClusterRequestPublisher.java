package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cluster.ClusterRequestConsumingPublisher;
import com.bhf.aeroncache.services.cluster.ClusterRequestPublisher;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
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
public class ObservingClusterRequestPublisher implements ClusterRequestPublisher, ClusterRequestConsumingPublisher, CacheResponseHandler, BlockingClusterRequestPublisher {

    private final ClusterMessagePublisher publisher;
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

    public ObservingClusterRequestPublisher onCreateCache(Consumer<CreateCacheResult<ReusableLong>> c) {
        createCacheConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onAddCacheEntry(Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c) {
        addCacheEntryConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onClearCache(Consumer<ClearCacheResult<ReusableLong>> c) {
        clearCacheConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onDeleteCache(Consumer<DeleteCacheResult<ReusableLong>> c) {
        deleteCacheConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onRemoveCacheEntry(Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c) {
        removeCacheEntryConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onGetCacheEntry(Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c) {
        getCacheEntryConsumer = c;
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
        createCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CreateCacheResult<ReusableLong> ReusableLongCreateCacheResult) {
                consumer.accept(ReusableLongCreateCacheResult);
            }
        });

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
    public void addCacheEntryBlocking(AeronCache cluster, long cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> consumer, String requestId) {
        addCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult) {
                consumer.accept(addCacheEntryResult);
            }
        });

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
    public void getCacheEntryBlocking(AeronCache cluster, long cacheId, String key, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> consumer, String requestId) {
        getCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult) {
                consumer.accept(getCacheEntryResult);
            }
        });

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
        clearCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(ClearCacheResult<ReusableLong> clearCacheEntryResult) {
                c.accept(clearCacheEntryResult);
            }
        });

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
        deleteCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(DeleteCacheResult<ReusableLong> deleteCacheEntryResult) {
                consumer.accept(deleteCacheEntryResult);
            }
        });

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
    public void removeCacheEntryBlocking(AeronCache cluster, long cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> consumer, String requestId) {
        removeCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
                consumer.accept(removeCacheEntryResult);
            }
        });

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
    public void getCacheEntriesBlocking(AeronCache cluster, long cacheId, Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> consumer, String requestId) {
        getCacheEntriesObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult) {
                consumer.accept(getCacheEntriesResult);
            }
        });

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
    public void getAllCacheStatsBlocking(AeronCache cluster, Consumer<CacheStatsResult<ReusableLong>> consumer, String requestId) {
        allCacheStatsObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheStatsResult<ReusableLong> getCacheStatsResult) {
                consumer.accept(getCacheStatsResult);
            }
        });

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
        cacheSubscribeObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheSubscriptionResult<ReusableLong> subscriptionResult) {
                c.accept(subscriptionResult);
            }
        });

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
        cacheUnsubscribeObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheUnsubscribeResult<ReusableLong> subscriptionResult) {
                c.accept(subscriptionResult);
            }
        });

        publisher.sendCacheUnsubscribe(cluster, requestId, cacheId);
    }


    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult) {
        var targetId = getCacheEntryResult.getRequestId();
        getCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntryResult));
        getCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (getCacheEntryConsumer != null) {
            getCacheEntryConsumer.accept(getCacheEntryResult);
        }
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult) {
        var targetId = getCacheEntriesResult.getRequestId();
        getCacheEntriesObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntriesResult));
        getCacheEntriesObservers.removeIf(p -> p.getId().equals(targetId));
        if (getCacheEntriesConsumer != null) {
            getCacheEntriesConsumer.accept(getCacheEntriesResult);
        }
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<ReusableLong> createCacheResult) {
        var targetId = createCacheResult.getRequestId();
        createCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(createCacheResult));
        createCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (createCacheConsumer != null) {
            createCacheConsumer.accept(createCacheResult);
        }
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult) {
        var targetId = addCacheEntryResult.getRequestId();
        addCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(addCacheEntryResult));
        addCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (addCacheEntryConsumer != null) {
            addCacheEntryConsumer.accept(addCacheEntryResult);
        }

    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
        var targetId = removeCacheEntryResult.getRequestId();
        removeCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(removeCacheEntryResult));
        removeCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (removeCacheEntryConsumer != null) {
            removeCacheEntryConsumer.accept(removeCacheEntryResult);
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableLong> clearCacheResult) {
        var targetId = clearCacheResult.getRequestId();
        clearCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(clearCacheResult));
        clearCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (clearCacheConsumer != null) {
            clearCacheConsumer.accept(clearCacheResult);
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableLong> deleteCacheResult) {
        var targetId = deleteCacheResult.getRequestId();
        deleteCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(deleteCacheResult));
        deleteCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (deleteCacheConsumer != null) {
            deleteCacheConsumer.accept(deleteCacheResult);
        }
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<ReusableLong> statsResult) {
        var targetId = statsResult.getRequestId();
        allCacheStatsObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(statsResult));
        allCacheStatsObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult) {
        var targetId = cacheSubscriptionResult.getRequestId();
        log.info("Got cache subscribe response on requestId {}", targetId);
        cacheSubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheSubscriptionResult));
        cacheSubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult) {
        var targetId = cacheUnsubscribeResult.getRequestId();
        log.info("Got cache unsubscribe response on requestId {}", targetId);
        cacheUnsubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheUnsubscribeResult));
        cacheUnsubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult) {

    }
}
