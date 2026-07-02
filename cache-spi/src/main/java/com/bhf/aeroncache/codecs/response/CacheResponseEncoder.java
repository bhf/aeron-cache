package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.consumer.HydratingPublicationConsumer;
import com.bhf.aeroncache.models.results.*;
import org.agrona.MutableDirectBuffer;

import java.util.Comparator;

public interface CacheResponseEncoder<I extends Reusable, K extends Reusable, V extends Reusable> {
    int encodeCacheCreationResult(I cacheId, CreateCacheResult<I> cacheCreationResult, MutableDirectBuffer egressBuffer);

    int encodeAddCacheEntryResult(I cacheId, K key, AddCacheEntryResult<I, K> addCacheEntryResult, MutableDirectBuffer egressBuffer);

    <VT extends Reusable> int encodeEntryUpdated(K key, VT value, AddCacheEntryResult<I, K> addCacheEntryResult, MutableDirectBuffer egressBuffer);

    <VT extends Reusable> int encodeCacheEntryResult(I cacheId, GetCacheEntryResult<I, K, VT> getCacheEntryResult, MutableDirectBuffer egressBuffer);

    <VT extends Reusable> int encodeAllCacheEntriesResult(I cacheId, GetAllCacheEntriesResult<I, K, VT> getAllCacheEntriesResult, MutableDirectBuffer egressBuffer);

    int encodeRemoveCacheEntryResult(I cacheId, K key, RemoveCacheEntryResult<I, K> removeCacheEntryResult, MutableDirectBuffer egressBuffer);

    int encodeCacheCleared(I cacheId, ClearCacheResult<I> clearCacheResult, MutableDirectBuffer egressBuffer);

    int encodeDeleteCache(I cacheId, DeleteCacheResult<I> deleteCacheResult, MutableDirectBuffer egressBuffer);

    int encodeCacheStatsResult(CacheStatsResult<I> cacheStatsResult, MutableDirectBuffer egressBuffer);

    <VT extends Reusable> void encodeCacheSubscriptionResult(CacheSubscriptionResult<I, K, VT> subscriptionRequestResult,
                                       MutableDirectBuffer egressBuffer, Comparator<K> keyComparator, HydratingPublicationConsumer consumer);

    int encodeCacheUnsubscribeResponse(CacheUnsubscribeResult<I> unsubscribeResponse, MutableDirectBuffer egressBuffer);

    int encodeBulkOpsResponse(BulkCacheOpsResult<I,K,V> bulkCacheOpsResult, MutableDirectBuffer egressBuffer);
}
