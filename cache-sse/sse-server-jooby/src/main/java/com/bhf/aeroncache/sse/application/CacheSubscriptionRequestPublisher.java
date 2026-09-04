package com.bhf.aeroncache.sse.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.models.consumer.IdentifiableConsumer;
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
public class CacheSubscriptionRequestPublisher<I extends Reusable, K extends Reusable, V extends Reusable> extends ObservingCacheRequestPublisher<I,K,V, String, String, String> implements SSEStatusHandler, CacheSubscriptions  {

    private final Map<String, List<IdentifiableConsumer<String, CacheUpdateEvent>>> cacheSubscriptions = new ConcurrentHashMap<>();

    public CacheSubscriptionRequestPublisher(CacheRequestPublisher rbPublisher) {
        super(rbPublisher);
    }


    /**
     * Subscribe to cache update.
     *
     * @param cluster     The cluster to use.
     * @param subscriptionFailureHandler   The handler for subscription failures.
     * @param cacheIds     The caches to subscribe too.
     * @param sseSessionId The SSE session ID.
     * @param requestId   The request ID.
     * @param consumer    The consumer of {@link CacheUpdateEvent}.
     */
    @Override
    public void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheIds,
                                 String sseSessionId, String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer) {
        List<IdentifiableConsumer<String, CacheUpdateEvent>> currentSubscribers;

        if (sendSnapshot) {
            sendCacheSubscriptionRequest(cluster, requestId, cacheIds, sendSnapshot, subscriptionFailureHandler, consumer);
        }

        for(var cacheId : cacheIds) {
            if (cacheSubscriptions.containsKey(cacheId)) {
                currentSubscribers = cacheSubscriptions.get(cacheId);
            } else {
                currentSubscribers = new CopyOnWriteArrayList<>();
                cacheSubscriptions.put(cacheId, currentSubscribers);

                if(!sendSnapshot) { // we don't want a snapshot but we're not subscribed
                    log.info("Sending request to cluster to subscribe to cache {}", cacheId);
                    sendCacheSubscriptionRequest(cluster, requestId, List.of(cacheId), sendSnapshot, subscriptionFailureHandler, consumer);
                }
            }

            log.info("Adding subscription for cache {}, send snapshot {} client session {}", cacheId, sendSnapshot, sseSessionId);
            currentSubscribers.add(new IdentifiableConsumer<>() {
                @Override
                public String getId() {
                    return sseSessionId;
                }

                @Override
                public void accept(CacheUpdateEvent cacheUpdateEvent) {
                    consumer.accept(cacheUpdateEvent);
                }
            });
        }
    }

    /**
     * Send a request to the cluster to subscribe to cache updates.
     *
     * @param cluster   The cluster to use.
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we want to subscribe too on the cluster side.
     */
    private void sendCacheSubscriptionRequest(AeronCache cluster, String requestId, List<String> cacheId, boolean sendSnapshot,
                                              Consumer<Void> subscriptionFailureHandler, Consumer<CacheUpdateEvent> streamingEventConsumer) {
        sendCacheSubscribe(requestId, cacheId, sendSnapshot, subscriptionResult -> {
            if (subscriptionResult.getStatus() != CacheOperationStatus.SUCCESS) {
                var errorMsg = "Couldn't subscribe to cache " + cacheId + ", status=" + subscriptionResult.getStatus();
                log.warn(errorMsg);

                if (subscriptionResult.getStatus() != CacheOperationStatus.DUPLICATE_SUBSCRIPTION) {
                    log.warn("Calling subscription failure handler to close SSE session");
                    subscriptionFailureHandler.accept(null);
                }
                else{
                    log.warn("Not calling subscription failure handler");
                }
            }

            if(subscriptionResult.getEntries()!=null && !subscriptionResult.getEntries().isEmpty()) {
                subscriptionResult.getEntries().forEach((k, v) -> {
                    CacheUpdateEvent.EventType eventType = CacheUpdateEvent.EventType.ADD_ITEM;
                    var ik = String.valueOf(k.value());
                    var iv = (v.value());
                    streamingEventConsumer.accept(new CacheUpdateEvent(subscriptionResult.getCacheId().toString(), eventType, ik, iv, requestId));
                });
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
                var sseSubscriptions = cacheSubscriptions.remove(cacheId);
                if (sseSubscriptions != null) {
                    log.info("Removed {} subscriptions to cacheId {}", sseSubscriptions.size(), cacheId);
                }
            }
        });
    }

    @Override
    public void handleSSEError(AeronCache cluster, String requestId, String sseSessionId) {
        removeSSESession(cluster, requestId, sseSessionId);
    }

    @Override
    public void handleSSEClosed(AeronCache cluster, String requestId, String sseSessionId) {
        removeSSESession(cluster, requestId, sseSessionId);
    }

    /**
     * Remove associated SSE session. If this causes the number of subscriptions
     * to the cache to hit 0, then unsubscribe to cache updates by sending a message
     * to the cluster.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param sseSessionId The SSE session we want to remove.
     */
    private void removeSSESession(AeronCache cluster, String requestId, String sseSessionId) {
        cacheSubscriptions.forEach((cacheId, sseConsumers) -> {
            var removed = sseConsumers.removeIf(p -> p.getId().equals(sseSessionId));
            if (removed) {
                log.info("Removed SSE session ID: {} subscription to cacheId: {}", sseSessionId, cacheId);
                if (sseConsumers.isEmpty()) {
                    log.info("Removed last SSE client subscription on cacheId: {}", cacheId);
                    sendCacheUnsubscribeRequest(cluster, requestId, cacheId);
                    cacheSubscriptions.remove(cacheId);
                }
            }
        });
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
        log.info("Got cache cleared to send to sse");
        var subscribers = cacheSubscriptions.get(clearCacheResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(clearCacheResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.CLEAR_CACHE;
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, null, null, clearCacheResult.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
        log.info("Got cache deleted to send to sse");
        var subscribers = cacheSubscriptions.get(deleteCacheResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(deleteCacheResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.DELETE_CACHE;
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, null, null, deleteCacheResult.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
        log.info("Got cache entry removed to send to sse");
        var subscribers = cacheSubscriptions.get(removeCacheEntryResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(removeCacheEntryResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.REMOVE_ITEM;
            var key = removeCacheEntryResult.getKey().value().toString();
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, null, removeCacheEntryResult.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {
        log.info("Got cache entry updated to send to sse");
        var subscribers = cacheSubscriptions.get(cacheEntryUpdateResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(cacheEntryUpdateResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = cacheEntryUpdateResult.getKey().value().toString();
            var value = cacheEntryUpdateResult.getValue().value();
            subscribers.forEach(c -> {
                try {
                    c.accept(
                            new CacheUpdateEvent(cacheId, eventType, key, value, cacheEntryUpdateResult.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCounterIncremented(IncrementCounterResult<I, K> result) {
        log.info("Got counter incremented to send to sse");
        var subscribers = cacheSubscriptions.get(result.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(result.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = result.getKey().value().toString();
            var value = result.getCounterValue();
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, value, result.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCounterDecremented(DecrementCounterResult<I, K> result) {
        log.info("Got counter decremented to send to sse");
        var subscribers = cacheSubscriptions.get(result.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(result.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = result.getKey().value().toString();
            var value = result.getCounterValue();
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, value, result.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCounterSet(SetCounterResult<I, K> result) {
        log.info("Got counter set to send to sse");
        var subscribers = cacheSubscriptions.get(result.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(result.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = result.getKey().value().toString();
            var value = result.getCounterValue();
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, value, result.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }
}
