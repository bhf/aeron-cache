package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.models.consumer.IdentifiableConsumer;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.near.NearCache;
import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    private final Map<String, List<KeyFilteredConsumer>> cacheSubscriptions = new ConcurrentHashMap<>();

    /**
     * Tracks the {@code (cacheId, key, mode)} combinations already subscribed to at the cluster so that
     * a shared cluster session is not subscribed multiple times for the same combination.
     */
    private final Set<String> clusterSubscriptions = ConcurrentHashMap.newKeySet();

    /**
     * A single reused update carrier. Streaming events are produced and dispatched only on the cluster
     * egress thread and consumed synchronously by each session's writer, so one instance can be populated
     * once per update and fanned out to every subscriber without allocating (see {@link GatewayStreamUpdate}).
     */
    private final GatewayStreamUpdate streamUpdate = new GatewayStreamUpdate();

    /**
     * A second reused update carrier, used only to replay the near cache's contents to a newly subscribing
     * session. Replay runs on the gateway ingress thread (inside {@link #subscribeToCache}), whereas
     * {@link #streamUpdate} is only ever touched by the cluster egress thread; giving replay its own
     * instance keeps each flyweight owned by a single thread (see {@link GatewayStreamUpdate}).
     */
    private final GatewayStreamUpdate replayUpdate = new GatewayStreamUpdate();

    /**
     * Optional local near cache. When present, whole-cache hydrating subscriptions are served from this
     * local copy instead of asking the cluster for a per-subscriber snapshot. {@code null} disables the
     * near cache (the default).
     */
    private final NearCacheManager<Object> nearCacheManager;

    /**
     * The session id used for the single internal subscription that keeps the near cache warm for a cache,
     * independent of any client sessions.
     */
    private final String nearCacheSessionId;

    /** Caches currently kept warm in the near cache (subscribed by the near cache session). */
    private final Set<String> nearWarmCaches = ConcurrentHashMap.newKeySet();

    public GatewaySubscriptionPublisher(CacheRequestPublisher rbPublisher) {
        this(rbPublisher, null, null);
    }

    /**
     * @param rbPublisher        the request publisher.
     * @param nearCacheManager   the local near cache to warm and hydrate from, or {@code null} to disable.
     * @param nearCacheSessionId the session id for the internal near cache subscription (required when
     *                           {@code nearCacheManager} is non-null).
     */
    public GatewaySubscriptionPublisher(CacheRequestPublisher rbPublisher, NearCacheManager<Object> nearCacheManager,
                                        String nearCacheSessionId) {
        super(rbPublisher);
        this.nearCacheManager = nearCacheManager;
        this.nearCacheSessionId = nearCacheSessionId;
    }

    /**
     * Whether near-cache-backed hydration is enabled on this publisher.
     *
     * @return true if a near cache is configured.
     */
    public boolean isNearCacheEnabled() {
        return nearCacheManager != null;
    }

    private static String clusterSubscriptionKey(String cacheId, String key, SubscriptionMode mode) {
        return cacheId + '\u0001' + (key == null ? "" : key) + '\u0001' + mode.name();
    }

    /**
     * A subscriber consumer that only receives events for the specific keys it subscribed to. A {@code null}
     * or empty key set denotes a whole-cache subscription that receives updates for every key.
     */
    private static final class KeyFilteredConsumer implements IdentifiableConsumer<String, GatewayStreamUpdate> {
        private final String sessionId;
        private final Set<String> keys;
        private final Consumer<GatewayStreamUpdate> delegate;

        KeyFilteredConsumer(String sessionId, Set<String> keys, Consumer<GatewayStreamUpdate> delegate) {
            this.sessionId = sessionId;
            this.keys = keys;
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return sessionId;
        }

        @Override
        public void accept(GatewayStreamUpdate streamUpdate) {
            delegate.accept(streamUpdate);
        }

        boolean matches(String key) {
            return keys == null || keys.isEmpty() || (key != null && keys.contains(key));
        }
    }

    @Override
    public void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, List<String> cacheIds,
                                 String sessionId, String requestId, boolean sendSnapshot, Consumer<GatewayStreamUpdate> consumer) {
        subscribeToCache(cluster, subscriptionFailureHandler, () -> {}, cacheIds, null, SubscriptionMode.FULL, sessionId, requestId, sendSnapshot, consumer);
    }

    @Override
    public void subscribeToCache(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, Runnable subscriptionAckHandler,
                                 List<String> cacheIds, List<String> keys, SubscriptionMode mode, String sessionId,
                                 String requestId, boolean sendSnapshot, Consumer<GatewayStreamUpdate> consumer) {
        var effectiveMode = mode == null ? SubscriptionMode.FULL : mode;

        // Near-cache-backed hydration applies to FULL-mode, hydrating client subscriptions, whole-cache or
        // key-based; the internal near cache session itself always takes the plain path. A key-based
        // subscription is only served from the near cache when the cache is already warm (see below): a key
        // snapshot carries only the requested keys, so it can never warm a full local copy, and a key-based
        // subscription to a cold cache therefore takes the plain cluster path.
        boolean wholeCache = (keys == null);
        boolean nearEligible = isNearCacheEnabled() && sendSnapshot
                && effectiveMode == SubscriptionMode.FULL && !sessionId.equals(nearCacheSessionId);

        if (!nearEligible) {
            doSubscribe(cluster, subscriptionFailureHandler, subscriptionAckHandler, cacheIds, keys, effectiveMode,
                    sessionId, requestId, true, sendSnapshot, consumer);
            return;
        }

        // A cache whose local copy already holds data can hydrate this subscriber entirely from the near
        // cache, avoiding a per-subscriber cluster snapshot. Only one cluster subscription per cache ever
        // requests a snapshot: the first (cold) subscriber. Requesting a snapshot a second time returns
        // DUPLICATE_SUBSCRIPTION on the shared cluster session, which (for counter caches) carries no
        // entries - so we must never rely on a second snapshot subscribe.
        var distinctCaches = new LinkedHashSet<>(cacheIds);
        boolean allHaveLocalData = distinctCaches.stream().allMatch(nearCacheManager::contains);

        if (allHaveLocalData) {
            // Ride the near cache session's existing whole-cache cluster subscription without touching the
            // cluster, and hydrate this subscriber from the local copy - the whole cache, or just the keys it
            // asked for. Live updates (filtered to those keys) keep flowing through the shared subscription.
            doSubscribe(cluster, subscriptionFailureHandler, subscriptionAckHandler, cacheIds, keys, effectiveMode,
                    sessionId, requestId, false, false, consumer);
            var keysByCache = groupKeysByCache(cacheIds, keys);
            for (var cacheId : distinctCaches) {
                replayNearCache(cacheId, keysByCache.get(cacheId), requestId, consumer);
            }
        } else if (wholeCache) {
            // At least one cache is cold. Hydrate this subscriber from the cluster snapshot (the single
            // SUCCESS subscribe) while teeing its events into the near cache to warm it, then register the
            // internal near cache session (without its own snapshot) so the local copy stays warm and
            // pinned after this client disconnects.
            Consumer<GatewayStreamUpdate> teeConsumer = update -> {
                nearCacheManager.handleCacheUpdate(update.cacheId(), update.eventType(), update.key(), update.value());
                consumer.accept(update);
            };
            doSubscribe(cluster, subscriptionFailureHandler, subscriptionAckHandler, cacheIds, keys, effectiveMode,
                    sessionId, requestId, true, true, teeConsumer);
            for (var cacheId : distinctCaches) {
                if (nearWarmCaches.add(cacheId)) {
                    registerNearCacheSession(cluster, cacheId);
                }
            }
        } else {
            // A key-based subscription to a (partly) cold cache: a key snapshot only carries the requested
            // keys, so it cannot warm a full local copy. Take the plain cluster path and leave the near cache
            // as it is; a whole-cache subscription or an explicit prime will warm it.
            doSubscribe(cluster, subscriptionFailureHandler, subscriptionAckHandler, cacheIds, keys, effectiveMode,
                    sessionId, requestId, true, sendSnapshot, consumer);
        }
    }

    /**
     * Register the internal near cache session as a (non-snapshot) subscriber to a cache, so the shared
     * cluster subscription and the local copy survive client churn until near caching is explicitly
     * stopped. The local copy's initial contents are warmed by the triggering client's teed snapshot; the
     * near cache session only needs to keep receiving live updates, so it never requests its own snapshot.
     *
     * @param cluster The cluster to use.
     * @param cacheId The cache to keep warm.
     */
    private void registerNearCacheSession(AeronCache cluster, String cacheId) {
        log.info("Registering near cache session for cacheId {}", cacheId);
        var requestId = UUID.randomUUID().toString();
        Consumer<GatewayStreamUpdate> nearConsumer = update ->
                nearCacheManager.handleCacheUpdate(update.cacheId(), update.eventType(), update.key(), update.value());
        doSubscribe(cluster, v -> {}, () -> {}, List.of(cacheId), null, SubscriptionMode.FULL,
                nearCacheSessionId, requestId, true, false, nearConsumer);
    }

    /**
     * Prime the near cache for a cache up front, before any hydrating client subscription arrives, so that
     * the first such subscription rides the already-warm local copy instead of triggering a cold
     * per-subscriber cluster snapshot.
     *
     * <p>This registers the internal near cache session as the first (cold) subscriber and, unlike
     * {@link #registerNearCacheSession(AeronCache, String)} (which rides a client's teed snapshot), requests
     * the snapshot itself since there is no client to tee one from. The snapshot populates the local copy
     * asynchronously, after which the session keeps receiving live updates to stay warm until near caching
     * is stopped. A cache that is already warm (primed earlier or warmed by a client subscription) is left
     * untouched; the warm-set guard also prevents a second snapshot subscribe, which the shared cluster
     * session rejects as a {@code DUPLICATE_SUBSCRIPTION} carrying no entries.
     *
     * @param cluster the cluster to use.
     * @param cacheId the cache to prime.
     * @return true if priming was started, false if the cache was already warm or the near cache is disabled.
     */
    public boolean primeNearCache(AeronCache cluster, String cacheId) {
        if (!isNearCacheEnabled()) {
            return false;
        }
        if (!nearWarmCaches.add(cacheId)) {
            return false;
        }
        log.info("Priming near cache for cacheId {}", cacheId);
        var requestId = UUID.randomUUID().toString();
        Consumer<GatewayStreamUpdate> nearConsumer = update ->
                nearCacheManager.handleCacheUpdate(update.cacheId(), update.eventType(), update.key(), update.value());
        doSubscribe(cluster, v -> nearWarmCaches.remove(cacheId), () -> {}, List.of(cacheId), null,
                SubscriptionMode.FULL, nearCacheSessionId, requestId, true, true, nearConsumer);
        return true;
    }

    /**
     * Replay the current contents of a near-cached cache to a subscriber as {@code ADD_ITEM} events,
     * hydrating it from the local copy. Runs on the gateway ingress thread and so uses {@link #replayUpdate}
     * rather than the egress-owned {@link #streamUpdate}. A key-filtered subscriber receives only the keys it
     * asked for; a whole-cache subscriber (empty or {@code null} key set) receives every key.
     *
     * @param cacheId   The cache to replay.
     * @param keys      The keys to replay, or {@code null}/empty to replay the whole cache.
     * @param requestId The request ID to stamp the replayed events with.
     * @param consumer  The subscriber to hydrate.
     */
    private void replayNearCache(String cacheId, Set<String> keys, String requestId, Consumer<GatewayStreamUpdate> consumer) {
        NearCache<Object> nearCache = nearCacheManager.get(cacheId);
        if (nearCache == null) {
            return;
        }
        boolean allKeys = (keys == null || keys.isEmpty());
        nearCache.snapshot().forEach((key, value) -> {
            if (allKeys || keys.contains(key)) {
                replayUpdate.set(cacheId, CacheUpdateEvent.EventType.ADD_ITEM, key, value, requestId);
                consumer.accept(replayUpdate);
            }
        });
    }

    /**
     * List the caches currently kept warm in the near cache.
     *
     * @return a snapshot of the warm cache IDs.
     */
    public Set<String> warmNearCaches() {
        return Set.copyOf(nearWarmCaches);
    }

    /**
     * The number of items held locally for a near-cached cache.
     *
     * @param cacheId the cache to query.
     * @return the local item count, or 0 if the cache is not warm.
     */
    public int nearCacheItemCount(String cacheId) {
        if (nearCacheManager == null) {
            return 0;
        }
        var nearCache = nearCacheManager.get(cacheId);
        return nearCache == null ? 0 : nearCache.size();
    }

    /**
     * Stop near caching a cache: drop the internal near cache subscription (so the local copy stops being
     * updated and, if no client sessions remain, the cluster subscription is released) and remove the local
     * copy. Active client subscriptions are not affected.
     *
     * @param cluster the cluster to use.
     * @param cacheId the cache to stop near caching.
     * @return true if a local near cache copy existed and was removed.
     */
    public boolean stopNearCaching(AeronCache cluster, String cacheId) {
        nearWarmCaches.remove(cacheId);
        var requestId = UUID.randomUUID().toString();
        unsubscribeSession(cluster, requestId, nearCacheSessionId, cacheId);
        return nearCacheManager != null && nearCacheManager.remove(cacheId);
    }

    private void doSubscribe(AeronCache cluster, Consumer<Void> subscriptionFailureHandler, Runnable subscriptionAckHandler,
                             List<String> cacheIds, List<String> keys, SubscriptionMode mode, String sessionId,
                             String requestId, boolean clusterSubscribe, boolean sendSnapshot, Consumer<GatewayStreamUpdate> consumer) {
        var effectiveMode = mode == null ? SubscriptionMode.FULL : mode;

        // Group the requested keys per cache; an empty key set denotes a whole-cache subscription.
        Map<String, Set<String>> keysByCache = groupKeysByCache(cacheIds, keys);

        // Work out which (cacheId, key, mode) combinations the shared cluster session must subscribe to. When
        // riding an already-warm cache ({@code clusterSubscribe == false}) nothing is sent to the cluster: the
        // near cache session's existing whole-cache subscription already delivers the live updates, so we only
        // register the local per-session consumer below.
        List<String> subscribeCacheIds = new ArrayList<>();
        List<String> subscribeKeys = new ArrayList<>();
        List<SubscriptionMode> subscribeModes = new ArrayList<>();
        if (clusterSubscribe) {
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
        }

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

        // Nothing was sent to the cluster - either every requested subscription was already live, or we are
        // riding a warm cache locally - so the subscription is confirmed immediately. When a cluster request
        // was sent, the ack is fired from its confirmation callback instead.
        if (subscribeCacheIds.isEmpty()) {
            subscriptionAckHandler.run();
        }
    }

    /**
     * Group the requested keys per cache. An entry maps a cache id to the set of keys the subscriber asked
     * for in that cache; an empty set denotes a whole-cache subscription (every key).
     *
     * @param cacheIds the caches subscribed to (parallel to {@code keys}).
     * @param keys     the keys parallel to {@code cacheIds}; a {@code null} entry (or {@code null} list) is a
     *                 whole-cache subscription for that cache.
     * @return the keys grouped by cache id, in encounter order.
     */
    private static Map<String, Set<String>> groupKeysByCache(List<String> cacheIds, List<String> keys) {
        Map<String, Set<String>> keysByCache = new LinkedHashMap<>();
        for (int i = 0; i < cacheIds.size(); i++) {
            var cacheId = cacheIds.get(i);
            var key = (keys == null) ? null : keys.get(i);
            var keySet = keysByCache.computeIfAbsent(cacheId, x -> new LinkedHashSet<>());
            if (key != null) {
                keySet.add(key);
            }
        }
        return keysByCache;
    }

    private void sendCacheSubscriptionRequest(AeronCache cluster, String requestId, List<String> cacheId, List<String> keys,
                                              List<SubscriptionMode> modes, boolean sendSnapshot,
                                              Consumer<Void> subscriptionFailureHandler, Runnable subscriptionAckHandler,
                                              Consumer<GatewayStreamUpdate> streamingEventConsumer) {
        // The whole subscribe is one atomic cluster command, so the first confirmed result means every
        // requested subscription in the batch is live. Ack exactly once for the request.
        var acked = new java.util.concurrent.atomic.AtomicBoolean(false);
        sendCacheSubscribe(requestId, cacheId, keys, modes, sendSnapshot, subscriptionResult -> {
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

            var status = subscriptionResult.getStatus();
            if ((status == CacheOperationStatus.SUCCESS || status == CacheOperationStatus.DUPLICATE_SUBSCRIPTION)
                    && acked.compareAndSet(false, true)) {
                subscriptionAckHandler.run();
            }

            if (subscriptionResult.getEntries() != null && !subscriptionResult.getEntries().isEmpty()) {
                var snapshotCacheId = subscriptionResult.getCacheId().toString();
                subscriptionResult.getEntries().forEach((k, v) -> {
                    var ik = String.valueOf(k.value());
                    var iv = (v.value());
                    streamUpdate.set(snapshotCacheId, CacheUpdateEvent.EventType.ADD_ITEM, ik, iv, requestId);
                    streamingEventConsumer.accept(streamUpdate);
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
                clusterSubscriptions.removeIf(k -> k.startsWith(cacheId + '\u0001'));
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
                    clusterSubscriptions.removeIf(k -> k.startsWith(cacheId + '\u0001'));
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
            clusterSubscriptions.removeIf(k -> k.startsWith(cacheId + '\u0001'));
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
            streamUpdate.set(cacheId, CacheUpdateEvent.EventType.CLEAR_CACHE, null, null, clearCacheResult.getRequestId());
            subscribers.forEach(c -> {
                try {
                    c.accept(streamUpdate);
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
        // A deleted cache is no longer warm; the near cache copy is dropped by handleCacheUpdate.
        nearWarmCaches.remove(String.valueOf(deleteCacheResult.getCacheId().value()));
        var subscribers = cacheSubscriptions.get(deleteCacheResult.getCacheId().value());
        if (subscribers != null) {
            var cacheId = String.valueOf(deleteCacheResult.getCacheId());
            streamUpdate.set(cacheId, CacheUpdateEvent.EventType.DELETE_CACHE, null, null, deleteCacheResult.getRequestId());
            subscribers.forEach(c -> {
                try {
                    c.accept(streamUpdate);
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
            var key = removeCacheEntryResult.getKey().value().toString();
            streamUpdate.set(cacheId, CacheUpdateEvent.EventType.REMOVE_ITEM, key, null, removeCacheEntryResult.getRequestId());
            subscribers.forEach(c -> {
                if (!c.matches(key)) {
                    return;
                }
                try {
                    c.accept(streamUpdate);
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
            var eventType = cacheEntryUpdateResult.isPatch() ? CacheUpdateEvent.EventType.PATCH_ITEM : CacheUpdateEvent.EventType.ADD_ITEM;
            var key = cacheEntryUpdateResult.getKey().value().toString();
            var value = cacheEntryUpdateResult.getValue().value();
            streamUpdate.set(cacheId, eventType, key, value, cacheEntryUpdateResult.getRequestId());
            subscribers.forEach(c -> {
                if (!c.matches(key)) {
                    return;
                }
                try {
                    c.accept(streamUpdate);
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
            var key = keyReusable.value().toString();
            streamUpdate.set(cacheId, CacheUpdateEvent.EventType.ADD_ITEM, key, counterValue, requestId);
            subscribers.forEach(c -> {
                if (!c.matches(key)) {
                    return;
                }
                try {
                    c.accept(streamUpdate);
                } catch (Exception e) {
                    log.warn("Error dispatching counter event", e);
                }
            });
        }
    }
}
