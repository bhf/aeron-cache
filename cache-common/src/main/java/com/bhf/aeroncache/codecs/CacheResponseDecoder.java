package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import org.agrona.DirectBuffer;

public interface CacheResponseDecoder<I extends Reusable, K extends Reusable, V extends Reusable> {
    void decodeCacheCreated(CreateCacheResult<I> createCacheResult, DirectBuffer buffer, int offset);

    void decodeAllCacheEntriesResult(GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult, DirectBuffer buffer, int offset);

    void decodeGetCacheEntryResult(GetCacheEntryResult<I, K, V> getCacheEntryResult, DirectBuffer buffer, int offset);

    void decodeAddCacheEntryResult(AddCacheEntryResult<I, K> addCacheEntryResult, DirectBuffer buffer, int offset);

    void decodeCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult, DirectBuffer buffer, int offset);

    void decodeCacheCleared(ClearCacheResult<I> clearCacheResult, DirectBuffer buffer, int offset);

    void decodeCacheDeleted(DeleteCacheResult<I> deleteCacheResult, DirectBuffer buffer, int offset);

    void decodeAllCacheStatsResult(CacheStatsResult<I> cacheStatsResult, DirectBuffer buffer, int offset);

    void decodeCacheSubscribeResult(CacheSubscriptionResult<I> cacheSubscriptionResult, DirectBuffer buffer, int offset);

    void decodeCacheUnsubscribeResult(CacheUnsubscribeResult<I> cacheUnsubscribeResult, DirectBuffer buffer, int offset);

    void decodeCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult, DirectBuffer buffer, int offset);
}
