package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestConsumingPublisher;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Getter
@Setter
public class CacheResponseObservers implements CacheRequestConsumingPublisher {
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
    public void sendCreateCache(long cacheId, Consumer<CreateCacheResult<ReusableLong>> consumer, String requestId) {
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
    }

    @Override
    public void addCacheEntry(long cacheId, String key, String value, Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c, String requestId) {
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
    public void getCacheEntry(long cacheId, String key, Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c, String requestId) {
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
    public void deleteCache(long cacheId, Consumer<DeleteCacheResult<ReusableLong>> consumer, String requestId) {
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
    public void removeCacheEntry(long cacheId, String key, Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c, String requestId) {
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
    public void clearCache(long cacheId, Consumer<ClearCacheResult<ReusableLong>> c, String requestId) {
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
    public void getCacheEntries(long cacheId, Consumer<GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString>> c, String requestId) {
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
    public void getAllCacheStats(Consumer<CacheStatsResult<ReusableLong>> c, String requestId) {
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
    public void sendCacheSubscribe(long cacheId, Consumer<CacheSubscriptionResult<ReusableLong>> c, String requestId) {
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
    public void sendCacheUnsubscribe(long cacheId, Consumer<CacheUnsubscribeResult<ReusableLong>> c, String requestId) {
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
}