package com.bhf.aeroncache.services.subscription;

import com.bhf.aeroncache.models.CompoundCacheKey;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.CacheUnsubscribeRequestDetails;
import com.bhf.aeroncache.models.results.*;
import io.aeron.cluster.service.ClientSession;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.*;
import java.util.function.Supplier;

@Log4j2
public class CacheSubscriptionServiceImpl<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheSubscriptionService<I,K,V> {

    private final IdleStrategy idleStrategy;
    private final CacheSubscriptionResult<I,K,V> subscriptionResult;
    private final CacheUnsubscribeResult<I> unsubscribeResult;
    private final Supplier<I> indexSupplier;
    private final Supplier<K> keySupplier;
    private final Map<I, Set<ClientSession>> wholeCacheSubscriptions = new HashMap<>();
    private final Map<CompoundCacheKey<I, K>, Set<ClientSession>> keySubscriptions = new HashMap<>();
    private final CompoundCacheKey<I, K> keyLookupKey;
    private final Set<ClientSession> sessionUnionScratch = new HashSet<>();

    public CacheSubscriptionServiceImpl(IdleStrategy idleStrategy,
                                        CacheSubscriptionResult<I, K, V> subscriptionResult,
                                        CacheUnsubscribeResult<I> unsubscribeResult,
                                        Supplier<I> indexSupplier,
                                        Supplier<K> keySupplier) {
        this.idleStrategy = idleStrategy;
        this.subscriptionResult = subscriptionResult;
        this.unsubscribeResult = unsubscribeResult;
        this.indexSupplier = indexSupplier;
        this.keySupplier = keySupplier;
        this.keyLookupKey = new CompoundCacheKey<>(indexSupplier.get(), keySupplier.get());
    }

    @Override
    public CacheSubscriptionResult<I,K,V> subscribe(ClientSession session, I cacheId, String requestId) {
        Set<ClientSession> existingSubscribers = wholeCacheSubscriptions.get(cacheId);
        if (existingSubscribers == null) {
            existingSubscribers = new HashSet<>();
            var storedId = indexSupplier.get();
            storedId.copyFrom(cacheId);
            wholeCacheSubscriptions.put(storedId, existingSubscribers);
        }
        return recordSubscription(session, cacheId, existingSubscribers, requestId);
    }

    @Override
    public CacheSubscriptionResult<I,K,V> subscribe(ClientSession session, I cacheId, K key, String requestId) {
        keyLookupKey.clear();
        keyLookupKey.getCacheId().copyFrom(cacheId);
        keyLookupKey.getKey().copyFrom(key);

        Set<ClientSession> existingSubscribers = keySubscriptions.get(keyLookupKey);
        if (existingSubscribers == null) {
            existingSubscribers = new HashSet<>();
            var storedKey = new CompoundCacheKey<>(indexSupplier.get(), keySupplier.get());
            storedKey.getCacheId().copyFrom(cacheId);
            storedKey.getKey().copyFrom(key);
            keySubscriptions.put(storedKey, existingSubscribers);
        }
        return recordSubscription(session, cacheId, existingSubscribers, requestId);
    }

    private CacheSubscriptionResult<I,K,V> recordSubscription(ClientSession session, I cacheId,
                                                              Set<ClientSession> subscribers, String requestId) {
        subscriptionResult.clear();
        subscriptionResult.setRequestId(requestId);
        subscriptionResult.getCacheId().copyFrom(cacheId);
        if (subscribers.add(session)) {
            subscriptionResult.setStatus(CacheOperationStatus.SUCCESS);
        } else {
            subscriptionResult.setStatus(CacheOperationStatus.DUPLICATE_SUBSCRIPTION);
        }
        log.debug("Total subscriptions for cache {}: {}", cacheId, subscribers.size());
        return subscriptionResult;
    }

    @Override
    public CacheUnsubscribeResult<I> unsubscribe(CacheUnsubscribeRequestDetails<I> requestDetails,
                                                 ClientSession session) {
        unsubscribeResult.clear();
        unsubscribeResult.setRequestId(requestDetails.getRequestId());
        var cacheId = requestDetails.getCacheId();
        unsubscribeResult.getCacheId().copyFrom(cacheId);

        var subscribers = wholeCacheSubscriptions.get(cacheId);
        if (subscribers != null) {
            if (subscribers.remove(session)) {
                log.info("Unsubscribed on cache {}, session {}", cacheId, session.id());
                unsubscribeResult.setStatus(CacheOperationStatus.SUCCESS);
            } else {
                unsubscribeResult.setStatus(CacheOperationStatus.UNKNOWN_SUBSCRIPTION);
            }
        } else {
            unsubscribeResult.setStatus(CacheOperationStatus.UNKNOWN_CACHE);
        }

        return unsubscribeResult;
    }

    void sendMessage(final ClientSession session, MutableDirectBuffer msgBuffer, int len) {
        while (session.offer(msgBuffer, 0, len) < 0) {
            idleStrategy.idle();
        }
    }

    /**
     * Sessions that should receive an update for a specific key within a cache.
     * @param cacheId
     * @param key
     * @return
     */
    private Set<ClientSession> getSessionsForKey(I cacheId, K key) {
        var wholeCacheSessions = wholeCacheSubscriptions.getOrDefault(cacheId, Collections.emptySet());

        keyLookupKey.clear();
        keyLookupKey.getCacheId().copyFrom(cacheId);
        keyLookupKey.getKey().copyFrom(key);
        var keySessions = keySubscriptions.getOrDefault(keyLookupKey, Collections.emptySet());

        if (keySessions.isEmpty()) {
            return wholeCacheSessions;
        }
        if (wholeCacheSessions.isEmpty()) {
            return keySessions;
        }
        sessionUnionScratch.clear();
        sessionUnionScratch.addAll(wholeCacheSessions);
        sessionUnionScratch.addAll(keySessions);
        return sessionUnionScratch;
    }

    /**
     * All sessions subscribed to a cache, regardless of key.
     * @param cacheId
     * @return
     */
    private Set<ClientSession> getAllSessionsForCache(I cacheId) {
        var wholeCacheSessions = wholeCacheSubscriptions.getOrDefault(cacheId, Collections.emptySet());

        boolean keyMatched = false;
        for (var entry : keySubscriptions.entrySet()) {
            if (entry.getKey().getCacheId().equals(cacheId)) {
                if (!keyMatched) {
                    keyMatched = true;
                    sessionUnionScratch.clear();
                }
                sessionUnionScratch.addAll(entry.getValue());
            }
        }

        if (!keyMatched) {
            return wholeCacheSessions;
        }
        sessionUnionScratch.addAll(wholeCacheSessions);
        return sessionUnionScratch;
    }

    @Override
    public void handleDeleteCache(DeleteCacheResult<I> requestDetails, MutableDirectBuffer egressBuffer,
                                  int length,
                                  long excludeSessionId) {
        log.info("CHECKING SESSIONS FOR SENDING DELETE ON CACHE {}", requestDetails.getCacheId());
        for (var session : getAllSessionsForCache(requestDetails.getCacheId())) {
            log.info("CHECKING SESSION {}", session.id());
            if (session.id() != excludeSessionId) {
                log.info("Sending delete cache update to session: {}", session.id());
                sendMessage(session, egressBuffer, length);
            }
        }
    }

    @Override
    public void handleClearCache(ClearCacheResult<I> clearCacheResult, MutableDirectBuffer egressBuffer,
                                 int length,
                                 long excludeSessionId) {
        for (var session : getAllSessionsForCache(clearCacheResult.getCacheId())) {
            if (session.id() != excludeSessionId) {
                log.debug("Sending clear cache update to session: {}", session.id());
                sendMessage(session, egressBuffer, length);
            }
        }
    }

    @Override
    public void handleEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult,
                                   MutableDirectBuffer egressBuffer, int length, long excludeSessionId) {
        log.info("Sending entry removed to subscribers on cacheId {}", removeCacheEntryResult.getCacheId());
        for (var session : getSessionsForKey(removeCacheEntryResult.getCacheId(), removeCacheEntryResult.getKey())) {
            if (session.id() != excludeSessionId) {
                log.debug("Sending entry removed update to session: {}", session.id());
                sendMessage(session, egressBuffer, length);
            }
        }
    }

    @Override
    public void handleTimerEntryRemoved(RemoveCacheEntryResult<I, K> removeCacheEntryResult,
                                        MutableDirectBuffer egressBuffer, int length) {
        log.info("Sending entry removed on timer to subscribers on cacheId {}", removeCacheEntryResult.getCacheId());
        for (var session : getSessionsForKey(removeCacheEntryResult.getCacheId(), removeCacheEntryResult.getKey())) {
            log.debug("Sending entry removed on timer update to session: {}", session.id());
            sendMessage(session, egressBuffer, length);
        }
    }

    @Override
    public <VT extends Reusable> void handleEntryAdded(AddCacheEntryResult<I, K> addCacheEntryResult,
                                 MutableDirectBuffer egressBuffer, K key,
                                 VT value, int length) {
        log.info("Sending entry added to subscribers on cacheId {}", addCacheEntryResult.getCacheId());
        for (var session : getSessionsForKey(addCacheEntryResult.getCacheId(), key)) {
            log.debug("Sending entry added update to session: {}", session.id());
            sendMessage(session, egressBuffer, length);
        }
    }

    @Override
    public void handleCounterUpdated(I cacheId, K key, MutableDirectBuffer egressBuffer, int length) {
        log.info("Sending counter updated to subscribers on cacheId {}", cacheId);
        for (var session : getSessionsForKey(cacheId, key)) {
            log.debug("Sending counter updated to session: {}", session.id());
            sendMessage(session, egressBuffer, length);
        }
    }

    @Override
    public boolean hasSubscriber(I cacheId, K key) {
        return !getSessionsForKey(cacheId, key).isEmpty();
    }

    @Override
    public void onSessionClose(ClientSession session) {
        log.info("Handling client session closed, sessionId: {}", session.id());
        wholeCacheSubscriptions.forEach((cacheId, clientSessions) -> {
            if (clientSessions.remove(session)) {
                log.info("Removed whole-cache subscription for cache {}, sessionId: {}", cacheId, session.id());
            }
        });
        keySubscriptions.forEach((compoundKey, clientSessions) -> {
            if (clientSessions.remove(session)) {
                log.info("Removed key subscription for {}, sessionId: {}", compoundKey, session.id());
            }
        });
    }
}
