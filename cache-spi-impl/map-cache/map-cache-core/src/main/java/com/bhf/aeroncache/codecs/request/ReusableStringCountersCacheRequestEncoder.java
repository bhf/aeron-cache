package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.MutableDirectBuffer;

import java.util.List;

public class ReusableStringCountersCacheRequestEncoder implements CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> {

    @Override
    public int encodeIncrementCounterRequest(String requestId, ReusableString cacheId, ReusableString counterId, long amount, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeDecrementCounterRequest(String requestId, ReusableString cacheId, ReusableString counterId, long amount, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeSetCounterRequest(String requestId, ReusableString cacheId, ReusableString counterId, long counterValue, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeCreateCacheRequest(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeAddCacheEntry(String requestId, ReusableString cacheId, ReusableString key, ReusableLong value, long ttl, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeGetCacheEntry(String requestId, ReusableString cacheId, ReusableString key, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeClearCache(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeDeleteCache(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeRemoveCacheEntry(String requestId, ReusableString cacheId, ReusableString key, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeGetCacheEntries(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeCacheSubscribe(String requestId, List<ReusableString> cacheId, boolean sendSnapshot, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeCacheUnsubscribe(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeGetAllCacheStats(String requestId, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeBulkOperations(String requestId, BulkCacheOpsRequest request, MutableDirectBuffer msgBuffer) {
        return 0;
    }
}
