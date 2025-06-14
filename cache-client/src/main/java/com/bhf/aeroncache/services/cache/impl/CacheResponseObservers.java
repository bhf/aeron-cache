package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestConsumingPublisher;
import com.bhf.aeroncache.services.cache.CacheResponseHandler;
import com.bhf.aeroncache.types.ReusableLong;
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
    final List<IdentifiableConsumer<String, CreateCacheResult<ReusableLong>>> createCacheObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, CreateCacheResult<ReusableLong>>>();
    final List<IdentifiableConsumer<String, AddCacheEntryResult<ReusableLong, ReusableString>>> addCacheEntryObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, AddCacheEntryResult<ReusableLong, ReusableString>>>();
    final List<IdentifiableConsumer<String, GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>>> getCacheEntryObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>>>();
    final List<IdentifiableConsumer<String, DeleteCacheResult<ReusableLong>>> deleteCacheObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, DeleteCacheResult<ReusableLong>>>();
    final List<IdentifiableConsumer<String, RemoveCacheEntryResult<ReusableLong, ReusableString>>> removeCacheEntryObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, RemoveCacheEntryResult<ReusableLong, ReusableString>>>();
    final List<IdentifiableConsumer<String, ClearCacheResult<ReusableLong>>> clearCacheObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, ClearCacheResult<ReusableLong>>>();
    final List<IdentifiableConsumer<String, GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>>> getCacheEntriesObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>>>();
    final List<IdentifiableConsumer<String, CacheStatsResult<ReusableLong>>> allCacheStatsObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, CacheStatsResult<ReusableLong>>>();
    final List<IdentifiableConsumer<String, CacheSubscriptionResult<ReusableLong>>> cacheSubscribeObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, CacheSubscriptionResult<ReusableLong>>>();
    final List<IdentifiableConsumer<String, CacheUnsubscribeResult<ReusableLong>>> cacheUnsubscribeObservers = new CopyOnWriteArrayList<IdentifiableConsumer<String, CacheUnsubscribeResult<ReusableLong>>>();
    Consumer<CreateCacheResult<ReusableLong>> createCacheConsumer;
    Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> addCacheEntryConsumer;
    Consumer<ClearCacheResult<ReusableLong>> clearCacheConsumer;
    Consumer<DeleteCacheResult<ReusableLong>> deleteCacheConsumer;
    Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> removeCacheEntryConsumer;
    Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> getCacheEntryConsumer;
    Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> getCacheEntriesConsumer;

    public CacheResponseObservers() {
    }

    @Override
    public void sendCreateCache(String requestId, long cacheId, Consumer<CreateCacheResult<ReusableLong>> consumer) {
        createCacheObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CreateCacheResult<ReusableLong> createCacheResult) {
                consumer.accept(createCacheResult);
            }
        });
    }

    @Override
    public void addCacheEntry(String requestId, long cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c) {
        addCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult) {
                c.accept(addCacheEntryResult);
            }
        });

    }

    @Override
    public void getCacheEntry(String requestId, long cacheId, String key, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c) {
        getCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult) {
                c.accept(getCacheEntryResult);
            }
        });
    }

    @Override
    public void deleteCache(String requestId, long cacheId, Consumer<DeleteCacheResult<ReusableLong>> consumer) {
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
    }

    @Override
    public void removeCacheEntry(String requestId, long cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c) {
        removeCacheEntryObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
                c.accept(removeCacheEntryResult);
            }
        });
    }

    @Override
    public void clearCache(String requestId, long cacheId, Consumer<ClearCacheResult<ReusableLong>> c) {
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
    }

    @Override
    public void getCacheEntries(String requestId, long cacheId, Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> c) {
        getCacheEntriesObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult) {
                c.accept(getCacheEntriesResult);
            }
        });
    }

    @Override
    public void getAllCacheStats(String requestId, Consumer<CacheStatsResult<ReusableLong>> c) {
        allCacheStatsObservers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return requestId;
            }

            @Override
            public void accept(CacheStatsResult<ReusableLong> getCacheStatsResult) {
                c.accept(getCacheStatsResult);
            }
        });
    }

    @Override
    public void sendCacheSubscribe(String requestId, long cacheId, Consumer<CacheSubscriptionResult<ReusableLong>> c) {
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
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, long cacheId, Consumer<CacheUnsubscribeResult<ReusableLong>> c) {
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