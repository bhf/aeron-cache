package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cluster.ClusterRequestPublisher;
import com.bhf.aeroncache.consumer.IdentifiableConsumer;
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
 * Methods which accept a {@link Consumer} use a CoW observer style approach after wrapping
 * the consumer into an {@link IdentifiableConsumer} with an internally generated Id.
 */
@RequiredArgsConstructor
public class ClusterRequestObserver implements ClusterRequestPublisher {

    final ClusterMessagePublisher publisher;
    final List<IdentifiableConsumer<String, CreateCacheResult<Long>>> createCacheObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, AddCacheEntryResult<Long, String>>> addCacheEntryObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, GetCacheEntryResult<Long, String, String>>> getCacheEntryObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, DeleteCacheResult<Long>>> deleteCacheObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, RemoveCacheEntryResult<Long, String>>> removeCacheEntryObservers = new CopyOnWriteArrayList<>();

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
}
