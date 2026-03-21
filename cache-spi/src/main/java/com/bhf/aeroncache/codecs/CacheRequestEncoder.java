package com.bhf.aeroncache.codecs;

import org.agrona.MutableDirectBuffer;

public interface CacheRequestEncoder<I, K, V> {
    int encodeCreateCacheRequest(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, MutableDirectBuffer msgBuffer);

    int encodeGetCacheEntry(String requestId, I cacheId, K key, MutableDirectBuffer msgBuffer);

    int encodeClearCache(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeDeleteCache(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeRemoveCacheEntry(String requestId, I cacheId, K key, MutableDirectBuffer msgBuffer);

    int encodeGetCacheEntries(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeCacheSubscribe(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeCacheUnsubscribe(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeGetAllCacheStats(String requestId, MutableDirectBuffer msgBuffer);
}
