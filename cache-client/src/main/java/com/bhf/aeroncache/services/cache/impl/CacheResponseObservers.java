package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestConsumingPublisher;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import com.bhf.aeroncache.types.ReusableString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Getter
@Setter
@Log4j2
public class CacheResponseObservers implements CacheRequestConsumingPublisher, CacheResponseHandler {
    final List<IdentifiableConsumer<String, CreateCacheResult<ReusableString>>> createCacheObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, CreateCacheResult<ReusableString>>>();
    final List<IdentifiableConsumer<String, AddCacheEntryResult<ReusableString, ReusableString>>> addCacheEntryObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, AddCacheEntryResult<ReusableString, ReusableString>>>();
    final List<IdentifiableConsumer<String, GetCacheEntryResult<ReusableString, ReusableString, ReusableString>>> getCacheEntryObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, GetCacheEntryResult<ReusableString, ReusableString, ReusableString>>>();
    final List<IdentifiableConsumer<String, DeleteCacheResult<ReusableString>>> deleteCacheObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, DeleteCacheResult<ReusableString>>>();
    final List<IdentifiableConsumer<String, RemoveCacheEntryResult<ReusableString, ReusableString>>> removeCacheEntryObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, RemoveCacheEntryResult<ReusableString, ReusableString>>>();
    final List<IdentifiableConsumer<String, ClearCacheResult<ReusableString>>> clearCacheObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, ClearCacheResult<ReusableString>>>();
    final List<IdentifiableConsumer<String, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>>> getCacheEntriesObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>>>();
    final List<IdentifiableConsumer<String, CacheStatsResult<ReusableString>>> allCacheStatsObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, CacheStatsResult<ReusableString>>>();
    final List<IdentifiableConsumer<String, CacheSubscriptionResult<ReusableString>>> cacheSubscribeObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, CacheSubscriptionResult<ReusableString>>>();
    final List<IdentifiableConsumer<String, CacheUnsubscribeResult<ReusableString>>> cacheUnsubscribeObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, CacheUnsubscribeResult<ReusableString>>>();
    Consumer<CreateCacheResult<ReusableString>> createCacheConsumer;
    Consumer<AddCacheEntryResult<ReusableString, ReusableString>> addCacheEntryConsumer;
    Consumer<ClearCacheResult<ReusableString>> clearCacheConsumer;
    Consumer<DeleteCacheResult<ReusableString>> deleteCacheConsumer;
    Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> removeCacheEntryConsumer;
    Consumer<GetCacheEntryResult<ReusableString, ReusableString, ReusableString>> getCacheEntryConsumer;
    Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>> getCacheEntriesConsumer;

    public CacheResponseObservers() {
    }

    @Override
    public void sendCreateCache(String requestId, String cacheId, Consumer<CreateCacheResult<ReusableString>> consumer) {
        createCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CreateCacheResult<ReusableString> createCacheResult) {
                consumer.accept(createCacheResult);
            }
        });
    }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableString, ReusableString>> c) {
        addCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult) {
                c.accept(addCacheEntryResult);
            }
        });

    }

    @Override
    public void getCacheEntry(String requestId, String cacheId, String key, Consumer<GetCacheEntryResult<ReusableString, ReusableString, ReusableString>> c) {
        getCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult) {
                c.accept(getCacheEntryResult);
            }
        });
    }

    @Override
    public void deleteCache(String requestId, String cacheId, Consumer<DeleteCacheResult<ReusableString>> consumer) {
        deleteCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(DeleteCacheResult<ReusableString> deleteCacheEntryResult) {
                consumer.accept(deleteCacheEntryResult);
            }
        });
    }

    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> c) {
        removeCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult) {
                c.accept(removeCacheEntryResult);
            }
        });
    }

    @Override
    public void clearCache(String requestId, String cacheId, Consumer<ClearCacheResult<ReusableString>> c) {
        clearCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(ClearCacheResult<ReusableString> clearCacheEntryResult) {
                c.accept(clearCacheEntryResult);
            }
        });
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId, Consumer<GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>> c) {
        getCacheEntriesObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult) {
                c.accept(getCacheEntriesResult);
            }
        });
    }

    @Override
    public void getAllCacheStats(String requestId, Consumer<CacheStatsResult<ReusableString>> c) {
        allCacheStatsObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheStatsResult<ReusableString> getCacheStatsResult) {
                c.accept(getCacheStatsResult);
            }
        });
    }

    @Override
    public void sendCacheSubscribe(String requestId, String cacheId, Consumer<CacheSubscriptionResult<ReusableString>> c) {
        cacheSubscribeObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheSubscriptionResult<ReusableString> subscriptionResult) {
                c.accept(subscriptionResult);
            }
        });
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId, Consumer<CacheUnsubscribeResult<ReusableString>> c) {
        cacheUnsubscribeObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheUnsubscribeResult<ReusableString> subscriptionResult) {
                c.accept(subscriptionResult);
            }
        });
    }

    @Override
    public void handleCacheEntryResult(GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult) {
        var targetId = getCacheEntryResult.getRequestId();
        getCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntryResult));
        getCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (getCacheEntryConsumer != null) {
            getCacheEntryConsumer.accept(getCacheEntryResult);
        }
    }

    @Override
    public void handleAllCacheEntries(GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult) {
        var targetId = getCacheEntriesResult.getRequestId();
        getCacheEntriesObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(getCacheEntriesResult));
        getCacheEntriesObservers.removeIf(p -> p.getId().equals(targetId));
        if (getCacheEntriesConsumer != null) {
            getCacheEntriesConsumer.accept(getCacheEntriesResult);
        }
    }

    @Override
    public void handleCacheCreated(CreateCacheResult<ReusableString> createCacheResult) {
        var targetId = createCacheResult.getRequestId();
        createCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(createCacheResult));
        createCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (createCacheConsumer != null) {
            createCacheConsumer.accept(createCacheResult);
        }
    }

    @Override
    public void handleCacheEntryCreated(AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult) {
        var targetId = addCacheEntryResult.getRequestId();
        addCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(addCacheEntryResult));
        addCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (addCacheEntryConsumer != null) {
            addCacheEntryConsumer.accept(addCacheEntryResult);
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult) {
        var targetId = removeCacheEntryResult.getRequestId();
        removeCacheEntryObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(removeCacheEntryResult));
        removeCacheEntryObservers.removeIf(p -> p.getId().equals(targetId));
        if (removeCacheEntryConsumer != null) {
            removeCacheEntryConsumer.accept(removeCacheEntryResult);
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableString> clearCacheResult) {
        var targetId = clearCacheResult.getRequestId();
        clearCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(clearCacheResult));
        clearCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (clearCacheConsumer != null) {
            clearCacheConsumer.accept(clearCacheResult);
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableString> deleteCacheResult) {
        var targetId = deleteCacheResult.getRequestId();
        deleteCacheObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(deleteCacheResult));
        deleteCacheObservers.removeIf(p -> p.getId().equals(targetId));
        if (deleteCacheConsumer != null) {
            deleteCacheConsumer.accept(deleteCacheResult);
        }
    }

    @Override
    public void handleAllCacheStats(CacheStatsResult<ReusableString> statsResult) {
        var targetId = statsResult.getRequestId();
        allCacheStatsObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(statsResult));
        allCacheStatsObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheSubscribeResponse(CacheSubscriptionResult<ReusableString> cacheSubscriptionResult) {
        var targetId = cacheSubscriptionResult.getRequestId();
        log.info("Got cache subscribe response on requestId {}", targetId);
        cacheSubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheSubscriptionResult));
        cacheSubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult) {
        var targetId = cacheUnsubscribeResult.getRequestId();
        log.info("Got cache unsubscribe response on requestId {}", targetId);
        cacheUnsubscribeObservers.stream().filter(p -> targetId.equals(p.getId())).forEach(c -> c.accept(cacheUnsubscribeResult));
        cacheUnsubscribeObservers.removeIf(p -> p.getId().equals(targetId));
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableString, ReusableString, ReusableString> cacheEntryUpdateResult) {

    }
}