package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import org.agrona.MutableDirectBuffer;

import java.util.ArrayList;
import java.util.List;

public interface CacheRequestEncoder<I, K, V> {
    int encodeCreateCacheRequest(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeAddCacheEntry(String requestId, I cacheId, K key, V value, long ttl, MutableDirectBuffer msgBuffer);

    int encodeGetCacheEntry(String requestId, I cacheId, K key, MutableDirectBuffer msgBuffer);

    int encodeClearCache(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeDeleteCache(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeRemoveCacheEntry(String requestId, I cacheId, K key, MutableDirectBuffer msgBuffer);

    int encodeCancelItemRemoval(String requestId, I cacheId, K key, MutableDirectBuffer msgBuffer);

    int encodePatchValue(String requestId, I cacheId, K key, V value, MutableDirectBuffer msgBuffer);

    int encodeGetCacheEntries(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    /**
     * Encode a whole-cache subscription request in {@link SubscriptionMode#FULL} mode. Delegates to the
     * key/mode aware overload with {@code null} keys (whole-cache) and {@link SubscriptionMode#FULL} modes.
     */
    default int encodeCacheSubscribe(String requestId, List<I> cacheId, boolean sendSnapshot, MutableDirectBuffer msgBuffer) {
        int n = cacheId.size();
        List<K> keys = new ArrayList<>(n);
        List<SubscriptionMode> modes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            keys.add(null);
            modes.add(SubscriptionMode.FULL);
        }
        return encodeCacheSubscribe(requestId, cacheId, keys, modes, sendSnapshot, msgBuffer);
    }

    /**
     * Encode a cache subscription request supporting per-cache subscription key and mode.
     *
     * @param requestId    The request ID.
     * @param cacheId      The caches to subscribe to.
     * @param keys         Parallel list of subscription keys; a {@code null} entry means a whole-cache subscription.
     * @param modes        Parallel list of subscription modes ({@link SubscriptionMode#FULL} or {@link SubscriptionMode#PATCH}).
     * @param sendSnapshot Whether to request an initial snapshot.
     * @param msgBuffer    The buffer to encode into.
     * @return The encoded length.
     */
    int encodeCacheSubscribe(String requestId, List<I> cacheId, List<K> keys, List<SubscriptionMode> modes, boolean sendSnapshot, MutableDirectBuffer msgBuffer);

    int encodeCacheUnsubscribe(String requestId, I cacheId, MutableDirectBuffer msgBuffer);

    int encodeGetAllCacheStats(String requestId, MutableDirectBuffer msgBuffer);

    int encodeBulkOperations(String requestId, BulkCacheOpsRequest request, MutableDirectBuffer msgBuffer);
}
