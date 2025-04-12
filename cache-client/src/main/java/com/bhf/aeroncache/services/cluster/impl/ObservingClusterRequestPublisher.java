package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cluster.ClusterRequestConsumingPublisher;
import com.bhf.aeroncache.services.cluster.ClusterRequestPublisher;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import io.aeron.cluster.client.AeronCluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.UUID;
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
public class ObservingClusterRequestPublisher implements ClusterRequestPublisher, ClusterRequestConsumingPublisher {

    private final ClusterMessagePublisher publisher;
    private final List<IdentifiableConsumer<String, CreateCacheResult<ReusableLong>>> createCacheObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, AddCacheEntryResult<ReusableLong, ReusableString>>> addCacheEntryObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>>> getCacheEntryObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, DeleteCacheResult<ReusableLong>>> deleteCacheObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, RemoveCacheEntryResult<ReusableLong, ReusableString>>> removeCacheEntryObservers = new CopyOnWriteArrayList<>();
    private final List<IdentifiableConsumer<String, ClearCacheResult<ReusableLong>>> clearCacheObservers = new CopyOnWriteArrayList<>();


    private Consumer<CreateCacheResult<ReusableLong>> createCacheConsumer;
    private Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> addCacheEntryConsumer;
    private Consumer<ClearCacheResult<ReusableLong>> clearCacheConsumer;
    private Consumer<DeleteCacheResult<ReusableLong>> deleteCacheConsumer;
    private Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> removeCacheEntryConsumer;
    private Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> getCacheEntryConsumer;

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
    public void sendCreateCache(AeronCluster cluster, String requestId, long cacheId) {
        publisher.sendCreateCache(cluster, requestId, cacheId);
    }

    @Override
    public void sendCreateCacheBlocking(AeronCluster cluster, String requestId, long cacheId) {
        publisher.sendCreateCacheBlocking(cluster, requestId, cacheId);
    }

    @Override
    public void sendCreateCacheBlocking(AeronCluster cluster, long cacheId, Consumer<CreateCacheResult<ReusableLong>> consumer) {
        var requestId = UUID.randomUUID().toString();
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
    public void addCacheEntry(AeronCluster cluster, String requestId, long cacheId, String key, String value) {
        publisher.addCacheEntry(cluster, requestId, cacheId, key, value);
    }

    @Override
    public void addCacheEntryBlocking(AeronCluster cluster, String requestId, long cacheId, String key, String value) {
        publisher.addCacheEntryBlocking(cluster, requestId, cacheId, key, value);
    }

    @Override
    public void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> consumer) {
        var requestId = UUID.randomUUID().toString();
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

        log.info("Adding cache entry");
        publisher.addCacheEntry(cluster, requestId, cacheId, key, value);
    }

    @Override
    public void getCacheEntry(AeronCluster cluster, String requestId, long cacheId, String key) {
        publisher.getCacheEntry(cluster, requestId, cacheId, key);
    }

    @Override
    public void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> consumer) {
        var requestId = UUID.randomUUID().toString();
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
    public void getCacheEntryBlocking(AeronCluster cluster, String requestId, long cacheId, String key) {
        publisher.getCacheEntryBlocking(cluster, requestId, cacheId, key);
    }

    @Override
    public void clearCache(AeronCluster cluster, String requestId, long cacheId) {
        publisher.clearCache(cluster, requestId, cacheId);
    }

    @Override
    public void clearCacheBlocking(AeronCluster cluster, String requestId, long cacheId) {
        publisher.clearCacheBlocking(cluster, requestId, cacheId);
    }

    @Override
    public void clearCacheBlocking(AeronCluster cluster, long cacheId, Consumer<ClearCacheResult<ReusableLong>> c) {
        var requestId = UUID.randomUUID().toString();
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
    public void deleteCache(AeronCluster cluster, String requestId, long cacheId) {
        publisher.deleteCache(cluster, requestId, cacheId);
    }

    @Override
    public void deleteCacheBlocking(AeronCluster cluster, String requestId, long cacheId) {
        publisher.deleteCacheBlocking(cluster, requestId, cacheId);
    }

    @Override
    public void deleteCacheBlocking(AeronCluster cluster, long cacheId, Consumer<DeleteCacheResult<ReusableLong>> consumer) {
        var requestId = UUID.randomUUID().toString();
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
    public void removeCacheEntry(AeronCluster cluster, String requestId, long cacheId, String key) {
        publisher.removeCacheEntry(cluster, requestId, cacheId, key);
    }

    @Override
    public void removeCacheEntryBlocking(AeronCluster cluster, String requestId, long cacheId, String key) {
        publisher.removeCacheEntryBlocking(cluster, requestId, cacheId, key);
    }

    @Override
    public void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> consumer) {
        var requestId = UUID.randomUUID().toString();
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
}
