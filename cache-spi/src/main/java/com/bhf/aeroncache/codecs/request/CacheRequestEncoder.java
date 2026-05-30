package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import org.agrona.MutableDirectBuffer;

public interface CacheRequestEncoder<I, K, V> {
    int encodeCreateCacheRequest(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, long ttl, MutableDirectBuffer msgBuffer);

    int encodeGetCacheEntry(String requestId, I cacheId, K key, MutableDirectBuffer msgBuffer);

    int encodeClearCache(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeDeleteCache(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeRemoveCacheEntry(String requestId, I cacheId, K key, MutableDirectBuffer msgBuffer);

    int encodeGetCacheEntries(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeCacheSubscribe(String requestId, I cacheId, boolean sendSnapshot, MutableDirectBuffer msgBuffer);

    int encodeCacheUnsubscribe(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeGetAllCacheStats(String requestId, MutableDirectBuffer msgBuffer);

    int encodeBulkOperations(String requestId, BulkCacheOpsRequest request, MutableDirectBuffer msgBuffer);
}
