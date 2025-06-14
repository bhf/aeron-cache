package com.bhf.aeroncache.ws.services.subscriptions;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.results.CacheEntryUpdateResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.DeleteCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import io.javalin.websocket.WsCloseStatus;
import io.javalin.websocket.WsContext;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manage subscriptions to Aeron Cache updates and send these
 * to connected websockets as {@link CacheUpdateEvent}s.
 */
@Log4j2
public class CacheSubscriptionRBService extends ObservingCacheRequestPublisher {

    private final Map<Long, List<IdentifiableConsumer<String, CacheUpdateEvent>>> cacheSubscriptions = new ConcurrentHashMap<>();

    public CacheSubscriptionRBService(CacheRequestPublisher publisher) {
        super(publisher);
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<ReusableLong> clearCacheResult) {
        log.info("Got cache cleared to send to ws");
        var subscribers = cacheSubscriptions.get(clearCacheResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(clearCacheResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.CLEAR_CACHE;
            subscribers.forEach(c -> c.accept(new CacheUpdateEvent(cacheId, eventType, null, null, clearCacheResult.getRequestId())));
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<ReusableLong> deleteCacheResult) {
        log.info("Got cache deleted to send to ws");
        var subscribers = cacheSubscriptions.get(deleteCacheResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(deleteCacheResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.DELETE_CACHE;
            subscribers.forEach(c -> c.accept(new CacheUpdateEvent(cacheId, eventType, null, null, deleteCacheResult.getRequestId())));
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult) {
        log.info("Got cache entry removed to send to ws");
        var subscribers = cacheSubscriptions.get(removeCacheEntryResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(removeCacheEntryResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.REMOVE_ITEM;
            var key = removeCacheEntryResult.getKey().value();
            subscribers.forEach(c -> c.accept(new CacheUpdateEvent(cacheId, eventType, key, null, removeCacheEntryResult.getRequestId())));
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult) {
        log.info("Got cache entry updated to send to ws");
        var subscribers = cacheSubscriptions.get(cacheEntryUpdateResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(cacheEntryUpdateResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = cacheEntryUpdateResult.getKey().value();
            var value = cacheEntryUpdateResult.getValue().value();
            subscribers.forEach(c -> c.accept(new CacheUpdateEvent(cacheId, eventType, key, value, cacheEntryUpdateResult.getRequestId())));
        }
    }

    /**
     * Subscribe to cache update.
     *
     * @param cluster     The cluster to use.
     * @param wsContext   The websocket context.
     * @param cacheId     The cache to subscribe too.
     * @param wsSessionId The websocket session ID.
     * @param requestId   The request ID.
     * @param consumer    The consumer of {@link CacheUpdateEvent}.
     */
    public void subscribeToCache(AeronCache cluster, WsContext wsContext, long cacheId, String wsSessionId, String requestId, Consumer<CacheUpdateEvent> consumer) {
        List<IdentifiableConsumer<String, CacheUpdateEvent>> currentSubscribers;
        if (cacheSubscriptions.containsKey(cacheId)) {
            currentSubscribers = cacheSubscriptions.get(cacheId);
        } else {
            currentSubscribers = new CopyOnWriteArrayList<>();
            cacheSubscriptions.put(cacheId, currentSubscribers);
            log.info("Sending request to cluster to subscribe to cache {}", cacheId);
            sendCacheSubscriptionRequest(cluster, requestId, cacheId, wsContext);
        }

        currentSubscribers.add(new IdentifiableConsumer<>() {
            @Override
            public String getId() {
                return wsSessionId;
            }

            @Override
            public void accept(CacheUpdateEvent cacheUpdateEvent) {
                consumer.accept(cacheUpdateEvent);
            }
        });
    }

    /**
     * Send a request to the cluster to subscribe to cache updates.
     *
     * @param cluster   The cluster to use.
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we want to subscribe too on the cluster side.
     */
    private void sendCacheSubscriptionRequest(AeronCache cluster, String requestId, long cacheId, WsContext wsContext) {
        sendCacheSubscribe(requestId, cacheId, subscriptionResult -> {
            if (subscriptionResult.getStatus() != OperationStatus.SUCCESS) {
                var errorMsg = STR."Couldn't subscribe to cache \{cacheId}, status=\{subscriptionResult.getStatus()}";
                log.warn(errorMsg);
                wsContext.closeSession(WsCloseStatus.SERVER_ERROR, errorMsg);
            }
        });
    }

    /**
     * Send a request to the cluster to unsubscribe to cache updated.
     *
     * @param cluster   The cluster to use.
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we want to unsubscribe too on the cluster side.
     */
    private void sendCacheUnsubscribeRequest(AeronCache cluster, String requestId, long cacheId) {
        sendCacheUnsubscribe(requestId, cacheId, unsubscribeResult -> {
            if (unsubscribeResult.getStatus() != OperationStatus.SUCCESS) {
                log.warn("Couldn't unsubscribe from cache {}, request ID {}", cacheId, requestId);
            } else {
                var wsSubscriptions = cacheSubscriptions.remove(cacheId);
                if (wsSubscriptions != null) {
                    log.info("Removed {} subscriptions to cacheId {}", wsSubscriptions.size(), cacheId);
                }
            }
        });
    }

    /**
     * Handle a websocket error. Removes any associated consumer
     * and cancels cache side subscriptions if necessary.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param wsSessionId The websocket session ID.
     */
    public void handleWsError(AeronCache cluster, String requestId, String wsSessionId) {
        removeWsSession(cluster, requestId, wsSessionId);
    }

    /**
     * Handle a websocket closing. Removes any associated consumer
     * * and cancels cache side subscriptions if necessary.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param wsSessionId The websocket session ID.
     */
    public void handleWsClosed(AeronCache cluster, String requestId, String wsSessionId) {
        removeWsSession(cluster, requestId, wsSessionId);
    }

    /**
     * Remove associated websocket session. If this causes the number of subscriptions
     * to the cache to hit 0, then unsubscribe to cache updates by sending a message
     * to the cluster.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param wsSessionId The websocket session we want to remove.
     */
    private void removeWsSession(AeronCache cluster, String requestId, String wsSessionId) {
        cacheSubscriptions.forEach((cacheId, wsConsumers) -> {
            var removed = wsConsumers.removeIf(p -> p.getId().equals(wsSessionId));
            if (removed) {
                log.info("Removed websocket session ID: {} subscription to cacheId: {}", wsSessionId, cacheId);
                if (wsConsumers.isEmpty()) {
                    log.info("Removed last websocket client subscription on cacheId: {}", cacheId);
                    sendCacheUnsubscribeRequest(cluster, requestId, cacheId);
                    cacheSubscriptions.remove(cacheId);
                }
            }
        });
    }

}
