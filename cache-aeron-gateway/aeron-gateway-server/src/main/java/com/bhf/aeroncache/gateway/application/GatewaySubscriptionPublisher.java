package com.bhf.aeroncache.gateway.application;

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

/**
 * Fan-out subscription publisher for the gateway. Mirrors the websocket/sse subscription
 * services: many gateway client sessions map onto a single cluster subscription per cache,
 * with optional snapshot hydration. Streaming updates are pushed to each session's
 * {@link CacheUpdateEvent} consumer, which writes an Aeron frame back to that session.
 */
@Log4j2
public class GatewaySubscriptionPublisher<I extends Reusable, K extends Reusable, V extends Reusable>
        extends ObservingCacheRequestPublisher<I, K, V, String, String, String>
        implements GatewayStatusHandler, GatewaySubscriptions {

    private final Map<String, List<IdentifiableConsumer<String, CacheUpdateEvent>>> cacheSubscriptions = new ConcurrentHashMap<>();

    public GatewaySubscriptionPublisher(CacheRequestPublisher rbPublisher) {
        super(rbPublisher);
    }

    @Override
    public void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheIds,
                                 String sessionId, String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer) {
        List<IdentifiableConsumer<String, CacheUpdateEvent>> currentSubscribers;

        if (sendSnapshot) {
            sendCacheSubscriptionRequest(cluster, requestId, cacheIds, sendSnapshot, subscriptionFailureHandler, consumer);
        }

        for (var cacheId : cacheIds) {
            if (cacheSubscriptions.containsKey(cacheId)) {
                currentSubscribers = cacheSubscriptions.get(cacheId);
            } else {
                currentSubscribers = new CopyOnWriteArrayList<>();
                cacheSubscriptions.put(cacheId, currentSubscribers);

                if (!sendSnapshot) { // we don't want a snapshot but we're not subscribed
                    log.info("Sending request to cluster to subscribe to cache {}", cacheId);
                    sendCacheSubscriptionRequest(cluster, requestId, List.of(cacheId), sendSnapshot, subscriptionFailureHandler, consumer);
                }
            }

            log.info("Adding subscription for cache {}, send snapshot {} client session {}", cacheId, sendSnapshot, sessionId);
            currentSubscribers.add(new IdentifiableConsumer<>() {
                @Override
                public String getId() {
                    return sessionId;
                }

                @Override
                public void accept(CacheUpdateEvent cacheUpdateEvent) {
                    consumer.accept(cacheUpdateEvent);
                }
            });
        }
    }

    private void sendCacheSubscriptionRequest(AeronCache cluster, String requestId, List<String> cacheId, boolean sendSnapshot,
                                              Consumer<Void> subscriptionFailureHandler, Consumer<CacheUpdateEvent> streamingEventConsumer) {
        sendCacheSubscribe(requestId, cacheId, sendSnapshot, subscriptionResult -> {
            if (subscriptionResult.getStatus() != CacheOperationStatus.SUCCESS) {
                var errorMsg = "Couldn't subscribe to cache " + cacheId + ", status=" + subscriptionResult.getStatus();
                log.warn(errorMsg);

                if (subscriptionResult.getStatus() != CacheOperationStatus.DUPLICATE_SUBSCRIPTION) {
                    log.warn("Calling subscription failure handler to close gateway session");
                    subscriptionFailureHandler.accept(null);
                } else {
                    log.warn("Not calling subscription failure handler");
                }
            }

            if (subscriptionResult.getEntries() != null && !subscriptionResult.getEntries().isEmpty()) {
                subscriptionResult.getEntries().forEach((k, v) -> {
                    CacheUpdateEvent.EventType eventType = CacheUpdateEvent.EventType.ADD_ITEM;
                    var ik = String.valueOf(k.value());
                    var iv = (v.value());
                    streamingEventConsumer.accept(new CacheUpdateEvent(subscriptionResult.getCacheId().toString(), eventType, ik, iv, requestId));
                });
            }
        });
    }

    private void sendCacheUnsubscribeRequest(AeronCache cluster, String requestId, String cacheId) {
        sendCacheUnsubscribe(requestId, cacheId, unsubscribeResult -> {
            if (unsubscribeResult.getStatus() != CacheOperationStatus.SUCCESS) {
                log.warn("Couldn't unsubscribe from cache {}, request ID {}", cacheId, requestId);
            } else {
                var subscriptions = cacheSubscriptions.remove(cacheId);
                if (subscriptions != null) {
                    log.info("Removed {} subscriptions to cacheId {}", subscriptions.size(), cacheId);
                }
            }
        });
    }

    @Override
    public void handleClosed(AeronCache cluster, String requestId, String sessionId) {
        cacheSubscriptions.forEach((cacheId, consumers) -> {
            var removed = consumers.removeIf(p -> p.getId().equals(sessionId));
            if (removed) {
                log.info("Removed gateway session ID: {} subscription to cacheId: {}", sessionId, cacheId);
                if (consumers.isEmpty()) {
                    log.info("Removed last gateway client subscription on cacheId: {}", cacheId);
                    sendCacheUnsubscribeRequest(cluster, requestId, cacheId);
                    cacheSubscriptions.remove(cacheId);
                }
            }
        });
    }

    /**
     * Remove a single session's subscription to a single cache in response to an
     * explicit unsubscribe request. If this was the last subscriber to the cache,
     * a cluster side unsubscribe is sent.
     *
     * @param cluster   The cluster to use.
     * @param requestId The request ID.
     * @param sessionId The gateway client session ID.
     * @param cacheId   The cache to unsubscribe from.
     */
    public void unsubscribeSession(AeronCache cluster, String requestId, String sessionId, String cacheId) {
        var consumers = cacheSubscriptions.get(cacheId);
        if (consumers == null) {
            return;
        }
        var removed = consumers.removeIf(p -> p.getId().equals(sessionId));
        if (removed && consumers.isEmpty()) {
            log.info("Removed last gateway client subscription on cacheId: {} via explicit unsubscribe", cacheId);
            sendCacheUnsubscribeRequest(cluster, requestId, cacheId);
            cacheSubscriptions.remove(cacheId);
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
        // Fire the request/response command callback (drives the gateway command response) before
        // dispatching streaming events to subscribers.
        super.handleCacheCleared(clearCacheResult);
        var subscribers = cacheSubscriptions.get(clearCacheResult.getCacheId().value());
        if (subscribers != null) {
            var cacheId = String.valueOf(clearCacheResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.CLEAR_CACHE;
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, null, null, clearCacheResult.getRequestId()));
                } catch (Exception e) {
                    log.warn("Error dispatching clear cache event", e);
                }
            });
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
        // Fire the request/response command callback (drives the gateway command response) before
        // dispatching streaming events to subscribers.
        super.handleCacheDeleted(deleteCacheResult);
        var subscribers = cacheSubscriptions.get(deleteCacheResult.getCacheId().value());
        if (subscribers != null) {
            var cacheId = String.valueOf(deleteCacheResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.DELETE_CACHE;
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, null, null, deleteCacheResult.getRequestId()));
                } catch (Exception e) {
                    log.warn("Error dispatching delete cache event", e);
                }
            });
        }
    }

    @Override
    public void handleCacheEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult) {
        // Fire the request/response command callback (drives the gateway command response) before
        // dispatching streaming events to subscribers.
        super.handleCacheEntryRemoved(removeCacheEntryResult);
        var subscribers = cacheSubscriptions.get(removeCacheEntryResult.getCacheId().value());
        if (subscribers != null) {
            var cacheId = String.valueOf(removeCacheEntryResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.REMOVE_ITEM;
            var key = removeCacheEntryResult.getKey().value().toString();
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, null, removeCacheEntryResult.getRequestId()));
                } catch (Exception e) {
                    log.warn("Error dispatching remove entry event", e);
                }
            });
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {
        var subscribers = cacheSubscriptions.get(cacheEntryUpdateResult.getCacheId().value());
        if (subscribers != null) {
            var cacheId = String.valueOf(cacheEntryUpdateResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = cacheEntryUpdateResult.getKey().value().toString();
            var value = cacheEntryUpdateResult.getValue().value();
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, value, cacheEntryUpdateResult.getRequestId()));
                } catch (Exception e) {
                    log.warn("Error dispatching entry update event", e);
                }
            });
        }
    }

    @Override
    public void handleCounterIncremented(IncrementCounterResult<I, K> result) {
        super.handleCounterIncremented(result);
        dispatchCounterEvent(result.getCacheId(), result.getKey(), result.getCounterValue(), result.getRequestId());
    }

    @Override
    public void handleCounterDecremented(DecrementCounterResult<I, K> result) {
        super.handleCounterDecremented(result);
        dispatchCounterEvent(result.getCacheId(), result.getKey(), result.getCounterValue(), result.getRequestId());
    }

    @Override
    public void handleCounterSet(SetCounterResult<I, K> result) {
        super.handleCounterSet(result);
        dispatchCounterEvent(result.getCacheId(), result.getKey(), result.getCounterValue(), result.getRequestId());
    }

    private void dispatchCounterEvent(I cacheIdReusable, K keyReusable, Object counterValue, String requestId) {
        var subscribers = cacheSubscriptions.get(cacheIdReusable.value());
        if (subscribers != null) {
            var cacheId = String.valueOf(cacheIdReusable);
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = keyReusable.value().toString();
            subscribers.forEach(c -> {
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, counterValue, requestId));
                } catch (Exception e) {
                    log.warn("Error dispatching counter event", e);
                }
            });
        }
    }
}
