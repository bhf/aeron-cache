package com.bhf.aeroncache.services.subscription;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import com.bhf.aeroncache.models.requests.CacheUnsubscribeRequestDetails;
import com.bhf.aeroncache.models.results.*;
import io.aeron.cluster.service.ClientSession;
import org.agrona.MutableDirectBuffer;

/**
 * Service for subscribing to caches.
 */
public interface CacheSubscriptionService<I extends Reusable, K extends Reusable, V extends Reusable> {

    /**
     * Subscribe to a cache.
     *
     * @param session The client session.
     * @param cacheId The cache to subscribe on.
     * @param requestId The requestId.
     * @return A response to the request for subscription.
     */
    CacheSubscriptionResult<I,K,V> subscribe(ClientSession session, I cacheId, String requestId);

    /**
     * Unsubscribe to a cache.
     *
     * @param requestDetails The unsubscribe details.
     * @param session
     * @return The response to a request to unsubscribe.
     */
    CacheUnsubscribeResult<I> unsubscribe(CacheUnsubscribeRequestDetails<I> requestDetails, ClientSession session);

    void handleDeleteCache(DeleteCacheResult<I> requestDetails, MutableDirectBuffer egressBuffer, int length, long excludeSessionId);

    void handleClearCache(ClearCacheResult<I> clearCacheResult, MutableDirectBuffer egressBuffer, int length, long excludeSessionId);

    void handleEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult, MutableDirectBuffer egressBuffer, int length, long excludeSessionId);

    void handleTimerEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult, MutableDirectBuffer egressBuffer, int length);

    <CT extends Reusable> void handleEntryAdded(AddCacheEntryResult<I, K> addCacheEntryResult, MutableDirectBuffer egressBuffer, K key, CT value, int length);

    void onSessionClose(ClientSession session);
}
