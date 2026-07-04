package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

public class ReusableStringCountersCacheResponseDecoder implements CountersCacheResponseDecoder<ReusableString,ReusableString, ReusableLong>{

    @Override
    public void decodeIncrementCounterResponse(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {

    }

    @Override
    public void decodeDecrementCounterResponse(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {

    }

    @Override
    public void decodeSetCounterResponse(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {

    }

    @Override
    public void decodeCacheCreated(DirectBuffer buffer, int offset, CreateCacheResult<ReusableString> createCacheResult) {

    }

    @Override
    public void decodeAllCacheEntriesResult(DirectBuffer buffer, int offset, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableLong> getCacheEntriesResult) {

    }

    @Override
    public void decodeGetCacheEntryResult(DirectBuffer buffer, int offset, GetCacheEntryResult<ReusableString, ReusableString, ReusableLong> getCacheEntryResult) {

    }

    @Override
    public void decodeAddCacheEntryResult(DirectBuffer buffer, int offset, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult) {

    }

    @Override
    public void decodeCacheEntryRemoved(DirectBuffer buffer, int offset, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult) {

    }

    @Override
    public void decodeCacheCleared(DirectBuffer buffer, int offset, ClearCacheResult<ReusableString> clearCacheResult) {

    }

    @Override
    public void decodeCacheDeleted(DirectBuffer buffer, int offset, DeleteCacheResult<ReusableString> deleteCacheResult) {

    }

    @Override
    public void decodeAllCacheStatsResult(DirectBuffer buffer, int offset, CacheStatsResult<ReusableString> cacheStatsResult) {

    }

    @Override
    public void decodeCacheSubscribeResult(DirectBuffer buffer, int offset, CacheSubscriptionResult<ReusableString, ReusableString, ReusableLong> cacheSubscriptionResult) {

    }

    @Override
    public void decodeCacheUnsubscribeResult(DirectBuffer buffer, int offset, CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult) {

    }

    @Override
    public void decodeCacheEntryUpdated(DirectBuffer buffer, int offset, CacheEntryUpdateResult<ReusableString, ReusableString, ReusableLong> cacheEntryUpdateResult) {

    }

    @Override
    public void decodeBulkCacheOpsResult(DirectBuffer buffer, int offset, BulkCacheOpsResult<ReusableString, ReusableString, ReusableLong> bulkCacheOpsResult) {

    }
}
