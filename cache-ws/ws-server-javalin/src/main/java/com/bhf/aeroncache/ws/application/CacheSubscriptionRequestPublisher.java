package com.bhf.aeroncache.ws.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Log4j2
public class CacheSubscriptionRequestPublisher<I extends Reusable, K extends Reusable, V extends Reusable> extends ObservingCacheRequestPublisher<I,K,V> implements WebsocketStatusHandler, CacheSubscriptions  {

    private final Map<String, List<IdentifiableConsumer<String, CacheUpdateEvent>>> cacheSubscriptions = new ConcurrentHashMap<>();

    public CacheSubscriptionRequestPublisher(CacheRequestPublisher rbPublisher) {
        super(rbPublisher);
    }


    /**
     * Subscribe to cache update.
     *
     * @param cluster     The cluster to use.
     * @param subscriptionFailureHandler   The handler for subscription failures.
     * @param cacheId     The cache to subscribe too.
     * @param wsSessionId The websocket session ID.
     * @param requestId   The request ID.
     * @param consumer    The consumer of {@link CacheUpdateEvent}.
     */
    @Override
    public void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, String cacheId, String wsSessionId, String requestId, Consumer<CacheUpdateEvent> consumer) {
        List<IdentifiableConsumer<String, CacheUpdateEvent>> currentSubscribers;
        if (cacheSubscriptions.containsKey(cacheId)) {
            currentSubscribers = cacheSubscriptions.get(cacheId);
        } else {
            currentSubscribers = new CopyOnWriteArrayList<>();
            cacheSubscriptions.put(cacheId, currentSubscribers);
            log.info("Sending request to cluster to subscribe to cache {}", cacheId);
            sendCacheSubscriptionRequest(cluster, requestId, cacheId, subscriptionFailureHandler);
        }

        log.info("Adding subscription for cache {}, client session {}", cacheId, wsSessionId);
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
    private void sendCacheSubscriptionRequest(AeronCache cluster, String requestId, String cacheId, Consumer<Void> subscriptionFailureHandler) {
        sendCacheSubscribe(requestId, cacheId, subscriptionResult -> {
            if (subscriptionResult.getStatus() != CacheOperationStatus.SUCCESS) {
                var errorMsg = STR."Couldn't subscribe to cache \{cacheId}, status=\{subscriptionResult.getStatus()}";
                log.warn(errorMsg);

                if (subscriptionResult.getStatus() != CacheOperationStatus.DUPLICATE_SUBSCRIPTION) {
                    log.warn("Calling subscription failure handler to close WS session");
                    subscriptionFailureHandler.accept(null);
                }
                else{
                    log.warn("Not calling subscription failure handler");
                }
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
    private void sendCacheUnsubscribeRequest(AeronCache cluster, String requestId, String cacheId) {
        sendCacheUnsubscribe(requestId, cacheId, unsubscribeResult -> {
            if (unsubscribeResult.getStatus() != CacheOperationStatus.SUCCESS) {
                log.warn("Couldn't unsubscribe from cache {}, request ID {}", cacheId, requestId);
            } else {
                var wsSubscriptions = cacheSubscriptions.remove(cacheId);
                if (wsSubscriptions != null) {
                    log.info("Removed {} subscriptions to cacheId {}", wsSubscriptions.size(), cacheId);
                }
            }
        });
    }

    @Override
    public void handleWsError(AeronCache cluster, String requestId, String wsSessionId) {
        removeWsSession(cluster, requestId, wsSessionId);
    }

    @Override
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

    @Override
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
        log.info("Got cache cleared to send to ws");
        var subscribers = cacheSubscriptions.get(clearCacheResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(clearCacheResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.CLEAR_CACHE;
            subscribers.forEach(c -> c.accept(new CacheUpdateEvent(cacheId, eventType, null, null, clearCacheResult.getRequestId())));
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
        log.info("Got cache deleted to send to ws");
        var subscribers = cacheSubscriptions.get(deleteCacheResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(deleteCacheResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.DELETE_CACHE;
            subscribers.forEach(c -> c.accept(new CacheUpdateEvent(cacheId, eventType, null, null, deleteCacheResult.getRequestId())));
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
        log.info("Got cache entry removed to send to ws");
        var subscribers = cacheSubscriptions.get(removeCacheEntryResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(removeCacheEntryResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.REMOVE_ITEM;
            var key = removeCacheEntryResult.getKey().value().toString();
            subscribers.forEach(c -> c.accept(
                    new CacheUpdateEvent(cacheId, eventType, key, null, removeCacheEntryResult.getRequestId())));
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {
        log.info("Got cache entry updated to send to ws");
        var subscribers = cacheSubscriptions.get(cacheEntryUpdateResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(cacheEntryUpdateResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = cacheEntryUpdateResult.getKey().value().toString();
            var value = cacheEntryUpdateResult.getValue().value().toString();
            subscribers.forEach(c -> {
                try {
                    c.accept(
                            new CacheUpdateEvent(cacheId, eventType, key, value, cacheEntryUpdateResult.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }
}
