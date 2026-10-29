package com.bhf.aeroncache.services.cache.subscription;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.models.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Transport-agnostic publisher that subscribes client sessions to cache update events and fans the
 * resulting streaming events out to the registered per-session consumers.
 *
 * <p>This holds the shared subscription machinery used by every streaming consumer of the cache: the
 * WebSocket and SSE interfaces and the HTTP near cache. Transport-specific concerns (for example
 * closing a WebSocket on failure) are layered on by thin subclasses that translate their lifecycle
 * callbacks into {@link #removeSession(AeronCache, String, String)} / {@link #unsubscribeSession}
 * calls; the HTTP near cache uses this class directly.
 *
 * <p>A shared cluster session is only subscribed once per {@code (cacheId, key, mode)} combination;
 * multiple client sessions interested in the same cache share that single cluster subscription and
 * each receives only the events matching the keys it asked for.
 */
@Log4j2
public class CacheSubscriptionRequestPublisher<I extends Reusable, K extends Reusable, V extends Reusable> extends ObservingCacheRequestPublisher<I,K,V, String, String, String> implements CacheSubscriptions {

    private final Map<String, List<KeyFilteredConsumer>> cacheSubscriptions = new ConcurrentHashMap<>();

    /**
     * Tracks the {@code (cacheId, key, mode)} combinations already subscribed to at the cluster so that
     * a shared cluster session is not subscribed multiple times for the same combination.
     */
    private final Set<String> clusterSubscriptions = ConcurrentHashMap.newKeySet();

    public CacheSubscriptionRequestPublisher(CacheRequestPublisher rbPublisher) {
        super(rbPublisher);
    }

    private static String clusterSubscriptionKey(String cacheId, String key, SubscriptionMode mode) {
        return cacheId + '\u0001' + (key == null ? "" : key) + '\u0001' + mode.name();
    }

    /**
     * A subscriber consumer that only receives events for the specific keys it subscribed to. A {@code null}
     * or empty key set denotes a whole-cache subscription that receives updates for every key.
     */
    private static final class KeyFilteredConsumer implements IdentifiableConsumer<String, CacheUpdateEvent> {
        private final String sessionId;
        private final Set<String> keys;
        private final Consumer<CacheUpdateEvent> delegate;

        KeyFilteredConsumer(String sessionId, Set<String> keys, Consumer<CacheUpdateEvent> delegate) {
            this.sessionId = sessionId;
            this.keys = keys;
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return sessionId;
        }

        @Override
        public void accept(CacheUpdateEvent cacheUpdateEvent) {
            delegate.accept(cacheUpdateEvent);
        }

        boolean matches(String key) {
            return keys == null || keys.isEmpty() || (key != null && keys.contains(key));
        }
    }

    /**
     * Subscribe a session to whole-cache updates for the given caches.
     *
     * @param cluster     The cluster to use.
     * @param subscriptionFailureHandler   The handler for subscription failures.
     * @param cacheIds     The caches to subscribe to.
     * @param sessionId   The client session ID.
     * @param requestId   The request ID.
     * @param sendSnapshot Whether to request initial state hydration.
     * @param consumer    The consumer of {@link CacheUpdateEvent}.
     */
    @Override
    public void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheIds,
                                 String sessionId, String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer) {
        subscribeToCache(cluster, subscriptionFailureHandler, () -> {}, cacheIds, null, SubscriptionMode.FULL, sessionId, requestId, sendSnapshot, consumer);
    }

    /**
     * Subscribe to cache updates for specific keys and a specific subscription mode.
     *
     * @param cluster                    The cluster to use.
     * @param subscriptionFailureHandler The handler for subscription failures.
     * @param cacheIds                   The caches to subscribe to (parallel to {@code keys}).
     * @param keys                       The subscription keys parallel to {@code cacheIds}; a {@code null} entry (or
     *                                   {@code null} list) means a whole-cache subscription for that cache.
     * @param mode                       The subscription mode ({@link SubscriptionMode#FULL} or {@link SubscriptionMode#PATCH}).
     * @param sessionId                  The client session ID.
     * @param requestId                  The request ID.
     * @param sendSnapshot               Whether to request initial state hydration.
     * @param consumer                   The consumer of {@link CacheUpdateEvent}.
     */
    @Override
    public void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, Runnable subscriptionAckHandler,
                                 List<String> cacheIds, List<String> keys, SubscriptionMode mode, String sessionId,
                                 String requestId, boolean sendSnapshot, Consumer<CacheUpdateEvent> consumer) {
        var effectiveMode = mode == null ? SubscriptionMode.FULL : mode;

        // Group the requested keys per cache; an empty key set denotes a whole-cache subscription.
        Map<String, Set<String>> keysByCache = new LinkedHashMap<>();
        for (int i = 0; i < cacheIds.size(); i++) {
            var cacheId = cacheIds.get(i);
            var key = (keys == null) ? null : keys.get(i);
            var keySet = keysByCache.computeIfAbsent(cacheId, x -> new LinkedHashSet<>());
            if (key != null) {
                keySet.add(key);
            }
        }

        // Work out which (cacheId, key, mode) combinations the shared cluster session must subscribe to.
        List<String> subscribeCacheIds = new ArrayList<>();
        List<String> subscribeKeys = new ArrayList<>();
        List<SubscriptionMode> subscribeModes = new ArrayList<>();
        keysByCache.forEach((cacheId, keySet) -> {
            if (keySet.isEmpty()) {
                boolean isNew = clusterSubscriptions.add(clusterSubscriptionKey(cacheId, null, effectiveMode));
                if (sendSnapshot || isNew) {
                    subscribeCacheIds.add(cacheId);
                    subscribeKeys.add(null);
                    subscribeModes.add(effectiveMode);
                }
            } else {
                for (var key : keySet) {
                    boolean isNew = clusterSubscriptions.add(clusterSubscriptionKey(cacheId, key, effectiveMode));
                    if (sendSnapshot || isNew) {
                        subscribeCacheIds.add(cacheId);
                        subscribeKeys.add(key);
                        subscribeModes.add(effectiveMode);
                    }
                }
            }
        });

        if (!subscribeCacheIds.isEmpty()) {
            log.info("Sending request to cluster to subscribe to caches {}, keys {}, mode {}", subscribeCacheIds, subscribeKeys, effectiveMode);
            sendCacheSubscriptionRequest(cluster, requestId, subscribeCacheIds, subscribeKeys, subscribeModes, sendSnapshot, subscriptionFailureHandler, subscriptionAckHandler, consumer);
        }

        // Register a per-session consumer for each cache, carrying the key set it wants to receive.
        keysByCache.forEach((cacheId, keySet) -> {
            var currentSubscribers = cacheSubscriptions.computeIfAbsent(cacheId, x -> new CopyOnWriteArrayList<>());
            log.info("Adding subscription for cache {}, keys {}, send snapshot {} client session {}", cacheId, keySet, sendSnapshot, sessionId);
            currentSubscribers.add(new KeyFilteredConsumer(sessionId, keySet.isEmpty() ? null : new LinkedHashSet<>(keySet), consumer));
        });

        // Nothing was sent to the cluster because every requested subscription was already live, so the
        // subscription is confirmed immediately. When a cluster request was sent, the ack is fired from
        // its confirmation callback instead.
        if (subscribeCacheIds.isEmpty()) {
            subscriptionAckHandler.run();
        }
    }

    /**
     * Send a request to the cluster to subscribe to cache updates.
     *
     * @param cluster   The cluster to use.
     * @param requestId The request ID.
     * @param cacheId   The IDs of the caches we want to subscribe to on the cluster side.
     * @param keys      The keys parallel to {@code cacheId}; a {@code null} entry means a whole-cache subscription.
     * @param modes     The subscription modes parallel to {@code cacheId}.
     */
    private void sendCacheSubscriptionRequest(AeronCache cluster, String requestId, List<String> cacheId, List<String> keys,
                                              List<SubscriptionMode> modes, boolean sendSnapshot,
                                              Consumer<Void> subscriptionFailureHandler, Runnable subscriptionAckHandler,
                                              Consumer<CacheUpdateEvent> streamingEventConsumer) {
        // The whole subscribe is one atomic cluster command, so the first confirmed result means every
        // requested subscription in the batch is live. Ack exactly once for the request.
        var acked = new java.util.concurrent.atomic.AtomicBoolean(false);
        sendCacheSubscribe(requestId, cacheId, keys, modes, sendSnapshot, subscriptionResult -> {
            if (subscriptionResult.getStatus() != CacheOperationStatus.SUCCESS) {
                var errorMsg = "Couldn't subscribe to cache " + cacheId + ", status=" + subscriptionResult.getStatus();
                log.warn(errorMsg);

                if (subscriptionResult.getStatus() != CacheOperationStatus.DUPLICATE_SUBSCRIPTION) {
                    log.warn("Calling subscription failure handler to close session");
                    subscriptionFailureHandler.accept(null);
                }
                else{
                    log.warn("Not calling subscription failure handler");
                }
            }

            var status = subscriptionResult.getStatus();
            if ((status == CacheOperationStatus.SUCCESS || status == CacheOperationStatus.DUPLICATE_SUBSCRIPTION)
                    && acked.compareAndSet(false, true)) {
                subscriptionAckHandler.run();
            }

            if(subscriptionResult.getEntries()!=null && !subscriptionResult.getEntries().isEmpty()){
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
     * Send a request to the cluster to unsubscribe from cache updates.
     *
     * @param cluster   The cluster to use.
     * @param requestId The request ID.
     * @param cacheId   The ID of the cache we want to unsubscribe from on the cluster side.
     */
    private void sendCacheUnsubscribeRequest(AeronCache cluster, String requestId, String cacheId) {
        sendCacheUnsubscribe(requestId, cacheId, unsubscribeResult -> {
            if (unsubscribeResult.getStatus() != CacheOperationStatus.SUCCESS) {
                log.warn("Couldn't unsubscribe from cache {}, request ID {}", cacheId, requestId);
            } else {
                var removedSubscriptions = cacheSubscriptions.remove(cacheId);
                if (removedSubscriptions != null) {
                    log.info("Removed {} subscriptions to cacheId {}", removedSubscriptions.size(), cacheId);
                }
                clusterSubscriptions.removeIf(k -> k.startsWith(cacheId + '\u0001'));
            }
        });
    }

    /**
     * Remove a client session's subscriptions across all caches. If removing a session leaves a cache
     * with no subscribers, a cluster side unsubscribe is sent for that cache.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param sessionId   The client session we want to remove.
     */
    protected void removeSession(AeronCache cluster, String requestId, String sessionId) {
        cacheSubscriptions.forEach((cacheId, consumers) -> {
            var removed = consumers.removeIf(p -> p.getId().equals(sessionId));
            if (removed) {
                log.info("Removed session ID: {} subscription to cacheId: {}", sessionId, cacheId);
                if (consumers.isEmpty()) {
                    log.info("Removed last client subscription on cacheId: {}", cacheId);
                    sendCacheUnsubscribeRequest(cluster, requestId, cacheId);
                    cacheSubscriptions.remove(cacheId);
                    clusterSubscriptions.removeIf(k -> k.startsWith(cacheId + '\u0001'));
                }
            }
        });
    }

    /**
     * Remove a single client session's subscription to a single cache in response to an explicit
     * unsubscribe request (used by the bidirectional protocol). If this was the last subscriber to the
     * cache, a cluster side unsubscribe is sent.
     *
     * @param cluster     The cluster to use.
     * @param requestId   The request ID.
     * @param sessionId   The client session we want to unsubscribe.
     * @param cacheId     The cache to unsubscribe from.
     */
    public void unsubscribeSession(AeronCache cluster, String requestId, String sessionId, String cacheId) {
        var consumers = cacheSubscriptions.get(cacheId);
        if (consumers == null) {
            return;
        }
        var removed = consumers.removeIf(p -> p.getId().equals(sessionId));
        if (removed && consumers.isEmpty()) {
            log.info("Removed last client subscription on cacheId: {} via explicit unsubscribe", cacheId);
            sendCacheUnsubscribeRequest(cluster, requestId, cacheId);
            cacheSubscriptions.remove(cacheId);
            clusterSubscriptions.removeIf(k -> k.startsWith(cacheId + '\u0001'));
        }
    }

    @Override
    public void handleCacheCleared(ClearCacheResult<I> clearCacheResult) {
        // Fire the request/response command callback (drives any pending command response) before
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

                }
            });
        }
    }

    @Override
    public void handleCacheDeleted(DeleteCacheResult<I> deleteCacheResult) {
        super.handleCacheDeleted(deleteCacheResult);
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
        super.handleCacheEntryRemoved(removeCacheEntryResult);
        var subscribers = cacheSubscriptions.get(removeCacheEntryResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(removeCacheEntryResult.getCacheId());
            var eventType = CacheUpdateEvent.EventType.REMOVE_ITEM;
            var key = removeCacheEntryResult.getKey().value().toString();
            subscribers.forEach(c -> {
                if (!c.matches(key)) {
                    return;
                }
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, null, removeCacheEntryResult.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCacheEntryUpdated(CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult) {
        var subscribers = cacheSubscriptions.get(cacheEntryUpdateResult.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(cacheEntryUpdateResult.getCacheId());
            var eventType = cacheEntryUpdateResult.isPatch() ? CacheUpdateEvent.EventType.PATCH_ITEM : CacheUpdateEvent.EventType.ADD_ITEM;
            var key = cacheEntryUpdateResult.getKey().value().toString();
            var value = cacheEntryUpdateResult.getValue().value();
            subscribers.forEach(c -> {
                if (!c.matches(key)) {
                    return;
                }
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
        super.handleCounterIncremented(result);
        var subscribers = cacheSubscriptions.get(result.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(result.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = result.getKey().value().toString();
            var value = result.getCounterValue();
            subscribers.forEach(c -> {
                if (!c.matches(key)) {
                    return;
                }
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, value, result.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCounterDecremented(DecrementCounterResult<I, K> result) {
        super.handleCounterDecremented(result);
        var subscribers = cacheSubscriptions.get(result.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(result.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = result.getKey().value().toString();
            var value = result.getCounterValue();
            subscribers.forEach(c -> {
                if (!c.matches(key)) {
                    return;
                }
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, value, result.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }

    @Override
    public void handleCounterSet(SetCounterResult<I, K> result) {
        super.handleCounterSet(result);
        var subscribers = cacheSubscriptions.get(result.getCacheId().value());

        if (subscribers != null) {
            var cacheId = String.valueOf(result.getCacheId());
            var eventType = CacheUpdateEvent.EventType.ADD_ITEM;
            var key = result.getKey().value().toString();
            var value = result.getCounterValue();
            subscribers.forEach(c -> {
                if (!c.matches(key)) {
                    return;
                }
                try {
                    c.accept(new CacheUpdateEvent(cacheId, eventType, key, value, result.getRequestId()));
                } catch (Exception e) {

                }
            });
        }
    }
}
