package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestConsumingPublisher;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Getter
@Setter
@Log4j2
public class CacheResponseObservers<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheRequestConsumingPublisher<I,K,V, String, String, String>, CacheResponseHandler<I,K,V> {

    final List<IdentifiableConsumer<String, CreateCacheResult<I>>> createCacheObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, AddCacheEntryResult<I, K>>> addCacheEntryObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, GetCacheEntryResult<I, K, V>>> getCacheEntryObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, DeleteCacheResult<I>>> deleteCacheObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, RemoveCacheEntryResult<I, K>>> removeCacheEntryObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, CancelItemRemovalResult<I, K>>> itemRemovalCancelledObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, ClearCacheResult<I>>> clearCacheObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, GetAllCacheEntriesResult<I, K, V>>> getCacheEntriesObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, CacheStatsResult<I>>> allCacheStatsObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, CacheSubscriptionResult<I,K,V>>> cacheSubscribeObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, CacheUnsubscribeResult<I>>> cacheUnsubscribeObservers = new CopyOnWriteArrayList<>();
    final List<IdentifiableConsumer<String, BulkCacheOpsResult<I,K,V>>> bulkOpsObservers = new CopyOnWriteArrayList<>();
    Consumer<CreateCacheResult<I>> createCacheConsumer;
    Consumer<AddCacheEntryResult<I, K>> addCacheEntryConsumer;
    Consumer<ClearCacheResult<I>> clearCacheConsumer;
    Consumer<DeleteCacheResult<I>> deleteCacheConsumer;
    Consumer<RemoveCacheEntryResult<I, K>> removeCacheEntryConsumer;
    Consumer<CancelItemRemovalResult<I, K>> itemRemovalCancelledConsumer;
    Consumer<GetCacheEntryResult<I, K, V>> getCacheEntryConsumer;
    Consumer<GetAllCacheEntriesResult<I, K, V>> getCacheEntriesConsumer;

    public CacheResponseObservers() {
    }

    @Override
    public void sendCreateCache(String requestId, String cacheId, Consumer<CreateCacheResult<I>> consumer) {
        createCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CreateCacheResult<I> createCacheResult) {
                consumer.accept(createCacheResult);
            }
        });
    }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, long ttl, Consumer<AddCacheEntryResult<I, K>> c) {
        addCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(AddCacheEntryResult<I, K> addCacheEntryResult) {
                c.accept(addCacheEntryResult);
            }
        });

    }

    @Override
    public void getCacheEntry(String requestId, String cacheId, String key, Consumer<GetCacheEntryResult<I, K, V>> c) {
        getCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(GetCacheEntryResult<I, K, V> getCacheEntryResult) {
                c.accept(getCacheEntryResult);
            }
        });
    }

    @Override
    public void deleteCache(String requestId, String cacheId, Consumer<DeleteCacheResult<I>> consumer) {
        deleteCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(DeleteCacheResult<I> deleteCacheEntryResult) {
                consumer.accept(deleteCacheEntryResult);
            }
        });
    }

    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key, Consumer<RemoveCacheEntryResult<I, K>> c) {
        removeCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
                c.accept(removeCacheEntryResult);
            }
        });
    }

    @Override
    public void cancelItemRemoval(String requestId, String cacheId, String key, Consumer<CancelItemRemovalResult<I, K>> c) {
        itemRemovalCancelledObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CancelItemRemovalResult<I, K> cancelItemRemovalResult) {
                c.accept(cancelItemRemovalResult);
            }
        });
    }

    @Override
    public void clearCache(String requestId, String cacheId, Consumer<ClearCacheResult<I>> c) {
        clearCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(ClearCacheResult<I> clearCacheEntryResult) {
                c.accept(clearCacheEntryResult);
            }
        });
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId, Consumer<GetAllCacheEntriesResult<I, K, V>> c) {
        getCacheEntriesObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult) {
                c.accept(getCacheEntriesResult);
            }
        });
    }

    @Override
    public void getAllCacheStats(String requestId, Consumer<CacheStatsResult<I>> c) {
        allCacheStatsObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheStatsResult<I> getCacheStatsResult) {
                c.accept(getCacheStatsResult);
            }
        });
    }

    @Override
    public void sendCacheSubscribe(String requestId, List<String> cacheId, boolean sendSnapshot, Consumer<CacheSubscriptionResult<I,K,V>> c) {
        cacheSubscribeObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheSubscriptionResult<I,K,V> subscriptionResult) {
                c.accept(subscriptionResult);
            }
        });
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId, Consumer<CacheUnsubscribeResult<I>> c) {
        cacheUnsubscribeObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheUnsubscribeResult<I> subscriptionResult) {
                c.accept(subscriptionResult);
            }
        });
    }

    @Override
    public void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request, Consumer<BulkCacheOpsResult<I,K,V>> c) {
        bulkOpsObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(BulkCacheOpsResult<I,K,V> bulkCacheOpsResult) {
                c.accept(bulkCacheOpsResult);
            }
        });
    }

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<I, K, V> getCacheEntryResult) {
        var targetId = getCacheEntryResult.getRequestId();
        getCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntryResult));
        getCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (getCacheEntryConsumer != null) {
            getCacheEntryConsumer.accept(getCacheEntryResult);
        }
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult) {
        var targetId = getCacheEntriesResult.getRequestId();
        getCacheEntriesObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntriesResult));
        getCacheEntriesObservers.removeIf(p -> p.getId().equals(targetId));
        if (getCacheEntriesConsumer != null) {
            getCacheEntriesConsumer.accept(getCacheEntriesResult);
        }
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<I> createCacheResult) {
        var targetId = createCacheResult.getRequestId();
        createCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(createCacheResult));
        createCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (createCacheConsumer != null) {
            createCacheConsumer.accept(createCacheResult);
        }
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<I, K> addCacheEntryResult) {
        var targetId = addCacheEntryResult.getRequestId();
        addCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(addCacheEntryResult));
        addCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (addCacheEntryConsumer != null) {
            addCacheEntryConsumer.accept(addCacheEntryResult);
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
        var targetId = removeCacheEntryResult.getRequestId();
        removeCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(removeCacheEntryResult));
        removeCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (removeCacheEntryConsumer != null) {
            removeCacheEntryConsumer.accept(removeCacheEntryResult);
        }
    }

    @Override
    public void handleItemRemovalCancelled(CancelItemRemovalResult<I, K> cancelItemRemovalResult) {
        var targetId = cancelItemRemovalResult.getRequestId();
        itemRemovalCancelledObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cancelItemRemovalResult));
        itemRemovalCancelledObservers.removeIf(p -> p.getId().equals(targetId));
        if (itemRemovalCancelledConsumer != null) {
            itemRemovalCancelledConsumer.accept(cancelItemRemovalResult);
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
        var targetId = clearCacheResult.getRequestId();
        clearCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(clearCacheResult));
        clearCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (clearCacheConsumer != null) {
            clearCacheConsumer.accept(clearCacheResult);
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
        var targetId = deleteCacheResult.getRequestId();
        deleteCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(deleteCacheResult));
        deleteCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (deleteCacheConsumer != null) {
            deleteCacheConsumer.accept(deleteCacheResult);
        }
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<I> statsResult) {
        var targetId = statsResult.getRequestId();
        allCacheStatsObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(statsResult));
        allCacheStatsObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<I,K,V> cacheSubscriptionResult) {
        var targetId = cacheSubscriptionResult.getRequestId();
        log.info("Got cache subscribe response on requestId {}", targetId);
        cacheSubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheSubscriptionResult));
        cacheSubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<I> cacheUnsubscribeResult) {
        var targetId = cacheUnsubscribeResult.getRequestId();
        log.info("Got cache unsubscribe response on requestId {}", targetId);
        cacheUnsubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheUnsubscribeResult));
        cacheUnsubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {

    }

    @Override
    public void handleBulkOperationsResult(BulkCacheOpsResult<I, K, V> bulkCacheOpsResult) {
        var targetId = bulkCacheOpsResult.getRequestId();
        log.info("Got bulk ops response on requestId {}", targetId);
        bulkOpsObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(bulkCacheOpsResult));
        bulkOpsObservers.removeIf(p -> p.getId().equals(targetId));
    }
}