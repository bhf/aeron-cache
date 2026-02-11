package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.*;
import org.agrona.DirectBuffer;

public interface CacheRequestDecoder<I extends Reusable, K extends Reusable, V extends Reusable> {
    void decodeCacheSubscriptionRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<I> cacheSubscribeRequestDetails);

    void decodeGetCreateCacheRequestDetails(DirectBuffer buffer, int offset, CreateCacheRequestDetails<I> createCacheRequestDetails);

    void decodeClearCacheRequest(DirectBuffer buffer, int offset, ClearCacheRequestDetails<I> clearCacheRequestDetails);

    void decodeRemoveCacheEntryRequest(DirectBuffer buffer, int offset, RemoveCacheEntryRequestDetails<I, K> removeCacheEntryRequestDetails);

    void decodeAddCacheEntryRequest(DirectBuffer buffer, int offset, AddCacheEntryRequestDetails<I, K, V> addCacheEntryRequestDetails);

    void decodeGetCacheEntryRequest(DirectBuffer buffer, int offset, GetCacheEntryRequestDetails<I, K> getCacheEntryRequestDetails);

    void decodeGetAllCacheEntriesRequest(DirectBuffer buffer, int offset, GetAllCacheEntriesRequestDetails<I> getAllCacheEntriesRequestDetails);

    void decodeGetDeleteCacheRequest(DirectBuffer buffer, int offset, DeleteCacheRequestDetails<I> deleteCacheRequestDetails);

    void decodeGetCacheStatsRequest(DirectBuffer buffer, int offset, GetCacheStatsRequestDetails getCacheStatsRequestDetails);

    void decodeGetCacheUnsubscribeRequest(DirectBuffer buffer, int offset, CacheUnsubscribeRequestDetails<I> cacheUnsubscribeRequestDetails);
}
