package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import org.agrona.DirectBuffer;

public interface CacheResponseDecoder<I extends Reusable, K extends Reusable, V extends Reusable> {
    void decodeCacheCreated(DirectBuffer buffer, int offset, CreateCacheResult<I> createCacheResult);

    void decodeAllCacheEntriesResult(DirectBuffer buffer, int offset, GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult);

    void decodeGetCacheEntryResult(DirectBuffer buffer, int offset, GetCacheEntryResult<I, K, V> getCacheEntryResult);

    void decodeAddCacheEntryResult(DirectBuffer buffer, int offset, AddCacheEntryResult<I, K> addCacheEntryResult);

    void decodeCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult, DirectBuffer buffer, int offset);

    void decodeCacheCleared(ClearCacheResult<I> clearCacheResult, DirectBuffer buffer, int offset);

    void decodeCacheDeleted(DeleteCacheResult<I> deleteCacheResult, DirectBuffer buffer, int offset);

    void decodeAllCacheStatsResult(CacheStatsResult<I> cacheStatsResult, DirectBuffer buffer, int offset);

    void decodeCacheSubscribeResult(CacheSubscriptionResult<I> cacheSubscriptionResult, DirectBuffer buffer, int offset);

    void decodeCacheUnsubscribeResult(CacheUnsubscribeResult<I> cacheUnsubscribeResult, DirectBuffer buffer, int offset);

    void decodeCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult, DirectBuffer buffer, int offset);
}
