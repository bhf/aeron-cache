package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

public class ReusableStringCountersCacheRequestDecoder implements CountersCacheRequestDecoder<ReusableString, ReusableString, ReusableLong> {
    @Override
    public void decodeIncrementCounterRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {

    }

    @Override
    public void decodeDecrementCounterRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {

    }

    @Override
    public void decodeSetCounterRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {

    }

    @Override
    public void decodeCacheSubscriptionRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {

    }

    @Override
    public void decodeGetCreateCacheRequestDetails(DirectBuffer buffer, int offset, CreateCacheRequestDetails<ReusableString> createCacheRequestDetails) {

    }

    @Override
    public void decodeClearCacheRequest(DirectBuffer buffer, int offset, ClearCacheRequestDetails<ReusableString> clearCacheRequestDetails) {

    }

    @Override
    public void decodeRemoveCacheEntryRequest(DirectBuffer buffer, int offset, RemoveCacheEntryRequestDetails<ReusableString, ReusableString> removeCacheEntryRequestDetails) {

    }

    @Override
    public <VT extends Reusable> void decodeAddCacheEntryRequest(DirectBuffer buffer, int offset, AddCacheEntryRequestDetails<ReusableString, ReusableString, VT> addCacheEntryRequestDetails) {

    }

    @Override
    public void decodeGetCacheEntryRequest(DirectBuffer buffer, int offset, GetCacheEntryRequestDetails<ReusableString, ReusableString> getCacheEntryRequestDetails) {

    }

    @Override
    public void decodeGetAllCacheEntriesRequest(DirectBuffer buffer, int offset, GetAllCacheEntriesRequestDetails<ReusableString> getAllCacheEntriesRequestDetails) {

    }

    @Override
    public void decodeGetDeleteCacheRequest(DirectBuffer buffer, int offset, DeleteCacheRequestDetails<ReusableString> deleteCacheRequestDetails) {

    }

    @Override
    public void decodeGetCacheStatsRequest(DirectBuffer buffer, int offset, GetCacheStatsRequestDetails getCacheStatsRequestDetails) {

    }

    @Override
    public void decodeGetCacheUnsubscribeRequest(DirectBuffer buffer, int offset, CacheUnsubscribeRequestDetails<ReusableString> cacheUnsubscribeRequestDetails) {

    }

    @Override
    public void decodeBulkCacheOperationsRequest(DirectBuffer buffer, int offset, BulkCacheOpsRequestDetails<ReusableString, ReusableString, ReusableLong> bulkCacheOpsRequestDetails) {

    }
}
