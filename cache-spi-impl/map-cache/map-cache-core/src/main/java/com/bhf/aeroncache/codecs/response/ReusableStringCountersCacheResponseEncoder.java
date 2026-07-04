package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.consumer.HydratingPublicationConsumer;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.MutableDirectBuffer;

import java.util.Comparator;

public class ReusableStringCountersCacheResponseEncoder implements CountersCacheResponseEncoder<ReusableString,ReusableString, ReusableLong>{
    @Override
    public int encodeIncrementResult(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeDecrementResult(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeSetCounterResult(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeCacheCreationResult(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeAddCacheEntryResult(ReusableString cacheId, ReusableString key, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public <VT extends Reusable> int encodeEntryUpdated(ReusableString key, VT value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public <VT extends Reusable> int encodeCacheEntryResult(ReusableString cacheId, GetCacheEntryResult<ReusableString, ReusableString, VT> getCacheEntryResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public <VT extends Reusable> int encodeAllCacheEntriesResult(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, VT> getAllCacheEntriesResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeRemoveCacheEntryResult(ReusableString cacheId, ReusableString key, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeCacheCleared(ReusableString cacheId, ClearCacheResult<ReusableString> clearCacheResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeDeleteCache(ReusableString cacheId, DeleteCacheResult<ReusableString> deleteCacheResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeCacheStatsResult(CacheStatsResult<ReusableString> cacheStatsResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public <VT extends Reusable> void encodeCacheSubscriptionResult(CacheSubscriptionResult<ReusableString, ReusableString, VT> subscriptionRequestResult, MutableDirectBuffer egressBuffer, Comparator<ReusableString> keyComparator, HydratingPublicationConsumer consumer) {

    }

    @Override
    public int encodeCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableString> unsubscribeResponse, MutableDirectBuffer egressBuffer) {
        return 0;
    }

    @Override
    public int encodeBulkOpsResponse(BulkCacheOpsResult<ReusableString, ReusableString, ReusableLong> bulkCacheOpsResult, MutableDirectBuffer egressBuffer) {
        return 0;
    }
}
