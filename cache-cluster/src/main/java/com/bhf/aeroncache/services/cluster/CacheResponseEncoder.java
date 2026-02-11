package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.DeleteCacheRequestDetails;
import com.bhf.aeroncache.models.results.*;
import org.agrona.MutableDirectBuffer;

public interface CacheResponseEncoder<I extends Reusable, K extends Reusable, V extends Reusable> {
    int encodeCacheCreationResult(I cacheId, CreateCacheResult<I> cacheCreationResult, MutableDirectBuffer egressBuffer);

    int encodeAddCacheEntryResult(I cacheId, K key, AddCacheEntryResult<I, K> addCacheEntryResult, MutableDirectBuffer egressBuffer);

    int encodeEntryUpdated(K key, V value, AddCacheEntryResult<I, K> addCacheEntryResult, MutableDirectBuffer egressBuffer);

    int encodeCacheEntryResult(I cacheId, GetCacheEntryResult<I, K, V> getCacheEntryResult, MutableDirectBuffer egressBuffer);

    int encodeAllCacheEntriesResult(I cacheId, GetAllCacheEntriesResult<I, K, V> getAllCacheEntriesResult, MutableDirectBuffer egressBuffer);

    int encodeRemoveCacheEntryResult(I cacheId, K key, RemoveCacheEntryResult<I, K> removeCacheEntryResult, MutableDirectBuffer egressBuffer);

    int encodeCacheCleared(I cacheId, ClearCacheResult<I> clearCacheResult, MutableDirectBuffer egressBuffer);

    int encodeDeleteCache(I cacheId, DeleteCacheResult<I> deleteCacheResult, DeleteCacheRequestDetails<I> requestDetails, MutableDirectBuffer egressBuffer);

    int encodeCacheStatsResult(CacheStatsResult<I> cacheStatsResult, MutableDirectBuffer egressBuffer);

    int encodeCacheSubscriptionResult(CacheSubscriptionResult<I> subscriptionRequestResult, MutableDirectBuffer egressBuffer);

    int encodeCacheUnsubscribeRequest(CacheUnsubscribeResult<I> unsubscribeResponse, MutableDirectBuffer egressBuffer);
}
