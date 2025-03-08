package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cluster.ClusterRequestConsumingPublisher;
import com.bhf.aeroncache.services.cluster.ClusterRequestPublisher;
import io.aeron.cluster.client.AeronCluster;
import lombok.RequiredArgsConstructor;

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
public class ObservingClusterRequestPublisher implements ClusterRequestPublisher, ClusterRequestConsumingPublisher {

    final ClusterMessagePublisher publisher;
    final List<IdentifiableConsumer<String, CreateCacheResult<Long>>> createCacheObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, AddCacheEntryResult<Long, String>>> addCacheEntryObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, GetCacheEntryResult<Long, String, String>>> getCacheEntryObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, DeleteCacheResult<Long>>> deleteCacheObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, RemoveCacheEntryResult<Long, String>>> removeCacheEntryObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, ClearCacheResult<Long>>> clearCacheObservers = new CopyOnWriteArrayList<>();


    private Consumer<CreateCacheResult<Long>> createCacheConsumer;
    private Consumer<AddCacheEntryResult<Long, String>> addCacheEntryConsumer;
    private Consumer<ClearCacheResult<Long>> clearCacheConsumer;
    private Consumer<DeleteCacheResult<Long>> deleteCacheConsumer;
    private Consumer<RemoveCacheEntryResult<Long, String>> removeCacheEntryConsumer;
    private Consumer<GetCacheEntryResult<Long, String, String>> getCacheEntryConsumer;

    public ObservingClusterRequestPublisher onCreateCache(Consumer<CreateCacheResult<Long>> c) {
        createCacheConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onAddCacheEntry(Consumer<AddCacheEntryResult<Long, String>> c) {
        addCacheEntryConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onClearCache(Consumer<ClearCacheResult<Long>> c) {
        clearCacheConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onDeleteCache(Consumer<DeleteCacheResult<Long>> c) {
        deleteCacheConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onRemoveCacheEntry(Consumer<RemoveCacheEntryResult<Long, String>> c) {
        removeCacheEntryConsumer = c;
        return this;
    }

    public ObservingClusterRequestPublisher onGetCacheEntry(Consumer<GetCacheEntryResult<Long, String, String>> c) {
        getCacheEntryConsumer = c;
        return this;
    }

    @Override
    public void sendCreateCache(AeronCluster cluster, long cacheId) {
        publisher.sendCreateCache(cluster, cacheId);
    }

    @Override
    public void sendCreateCacheBlocking(AeronCluster cluster, long cacheId) {
        publisher.sendCreateCacheBlocking(cluster, cacheId);
    }

    @Override
    public void sendCreateCacheBlocking(AeronCluster cluster, long cacheId, Consumer<CreateCacheResult<Long>> consumer) {
        createCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @Override
            public void accept(CreateCacheResult<Long> longCreateCacheResult) {
                consumer.accept(longCreateCacheResult);
            }
        });

        publisher.sendCreateCache(cluster, cacheId);
    }

    @Override
    public void addCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key, String value) {
        publisher.addCacheEntryNonBlocking(cluster, cacheId, key, value);
    }

    @Override
    public void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value) {
        publisher.addCacheEntryBlocking(cluster, cacheId, key, value);
    }

    @Override
    public void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value, Consumer<AddCacheEntryResult<Long, String>> consumer) {
        addCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @Override
            public void accept(AddCacheEntryResult<Long, String> addCacheEntryResult) {
                consumer.accept(addCacheEntryResult);
            }
        });

        publisher.addCacheEntryNonBlocking(cluster, cacheId, key, value);
    }

    @Override
    public void getCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key) {
        publisher.getCacheEntryNonBlocking(cluster, cacheId, key);
    }

    @Override
    public void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<GetCacheEntryResult<Long, String, String>> consumer) {
        getCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @Override
            public void accept(GetCacheEntryResult<Long, String, String> getCacheEntryResult) {
                consumer.accept(getCacheEntryResult);
            }
        });

        publisher.getCacheEntryNonBlocking(cluster, cacheId, key);
    }

    @Override
    public void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key) {
        publisher.getCacheEntryBlocking(cluster, cacheId, key);
    }

    @Override
    public void clearCacheNonBlocking(AeronCluster cluster, long cacheId) {
        publisher.clearCacheNonBlocking(cluster, cacheId);
    }

    @Override
    public void clearCacheBlocking(AeronCluster cluster, long cacheId) {
        publisher.clearCacheBlocking(cluster, cacheId);
    }

    @Override
    public void deleteCacheNonBlocking(AeronCluster cluster, long cacheId) {
        publisher.deleteCacheNonBlocking(cluster, cacheId);
    }

    @Override
    public void deleteCacheBlocking(AeronCluster cluster, long cacheId) {
        publisher.deleteCacheBlocking(cluster, cacheId);
    }

    @Override
    public void deleteCacheBlocking(AeronCluster cluster, long cacheId, Consumer<DeleteCacheResult<Long>> consumer) {
        deleteCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @Override
            public void accept(DeleteCacheResult<Long> deleteCacheEntryResult) {
                consumer.accept(deleteCacheEntryResult);
            }
        });

        publisher.deleteCacheNonBlocking(cluster, cacheId);
    }

    @Override
    public void removeCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key) {
        publisher.removeCacheEntryNonBlocking(cluster, cacheId, key);
    }

    @Override
    public void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key) {
        publisher.removeCacheEntryBlocking(cluster, cacheId, key);
    }

    @Override
    public void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<RemoveCacheEntryResult<Long, String>> consumer) {
        removeCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @Override
            public void accept(RemoveCacheEntryResult<Long, String> removeCacheEntryResult) {
                consumer.accept(removeCacheEntryResult);
            }
        });

        publisher.deleteCacheNonBlocking(cluster, cacheId);
    }

    /**
     * Handle a message indicating the value of a get operation on a particular key
     * and delegate it to any relevant consumer.
     *
     * @param getCacheEntryResult The result of getting something from the cache.
     */
    public void handleCacheEntryResult(GetCacheEntryResult<Long, String, String> getCacheEntryResult) {
        var expectedId = getCacheEntryResult.getCacheId();
        while (getCacheEntryObservers.iterator().hasNext()) {
            var next = getCacheEntryObservers.iterator().next();
            if (expectedId.equals(next.getId())) {
                next.accept(getCacheEntryResult);
                getCacheEntryObservers.iterator().remove();
            }
        }

        getCacheEntryConsumer.accept(getCacheEntryResult);
    }

    /**
     * Handle a message indicating a cache has been created and delegate it
     * to any relevant consumer.
     *
     * @param createCacheResult The result of creating a cache.
     */
    public void handleCacheCreated(CreateCacheResult<Long> createCacheResult) {
        var expectedId = createCacheResult.getCacheId();
        while (createCacheObservers.iterator().hasNext()) {
            var next = createCacheObservers.iterator().next();
            if (expectedId.equals(next.getId())) {
                next.accept(createCacheResult);
                createCacheObservers.iterator().remove();
            }
        }

        createCacheConsumer.accept(createCacheResult);
    }

    /**
     * Handle a message indicating a cache entry has been created and delegate it
     * to any relevant consumer.
     *
     * @param addCacheEntryResult The result of adding an entry to the cache.
     */
    public void handleCacheEntryCreated(AddCacheEntryResult<Long, String> addCacheEntryResult) {
        var expectedId = addCacheEntryResult.getCacheID();
        while (addCacheEntryObservers.iterator().hasNext()) {
            var next = addCacheEntryObservers.iterator().next();
            if (expectedId.equals(next.getId())) {
                next.accept(addCacheEntryResult);
                addCacheEntryObservers.iterator().remove();
            }
        }

        addCacheEntryConsumer.accept(addCacheEntryResult);
    }

    /**
     * Handle a message indicating a cache entry has been removed and delegate it
     * to any relevant consumer.
     *
     * @param removeCacheEntryResult The result of a cache entry removal.
     */
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<Long, String> removeCacheEntryResult) {
        var expectedId = removeCacheEntryResult.getCacheId();
        while (removeCacheEntryObservers.iterator().hasNext()) {
            var next = removeCacheEntryObservers.iterator().next();
            if (expectedId.equals(next.getId())) {
                next.accept(removeCacheEntryResult);
                removeCacheEntryObservers.iterator().remove();
            }
        }

        removeCacheEntryConsumer.accept(removeCacheEntryResult);
    }

    /**
     * Handle a message indicating a cache has been cleared and delegate it
     * to any relevant consumer.
     *
     * @param clearCacheResult The result of clearing a cache.
     */
    public void handleCacheCleared(ClearCacheResult<Long> clearCacheResult) {
        var expectedId = clearCacheResult.getCacheId();
        while (clearCacheObservers.iterator().hasNext()) {
            var next = clearCacheObservers.iterator().next();
            if (expectedId.equals(next.getId())) {
                next.accept(clearCacheResult);
                clearCacheObservers.iterator().remove();
            }
        }

        clearCacheConsumer.accept(clearCacheResult);
    }

    /**
     * Handle a message indicating a cache has been deleted and delegate it
     * to any relevant consumer.
     *
     * @param deleteCacheResult The result of deleting a cache.
     */
    public void handleCacheDeleted(DeleteCacheResult<Long> deleteCacheResult) {
        var expectedId = deleteCacheResult.getCacheId();
        while (deleteCacheObservers.iterator().hasNext()) {
            var next = deleteCacheObservers.iterator().next();
            if (expectedId.equals(next.getId())) {
                next.accept(deleteCacheResult);
                deleteCacheObservers.iterator().remove();
            }
        }

        deleteCacheConsumer.accept(deleteCacheResult);
    }
}
