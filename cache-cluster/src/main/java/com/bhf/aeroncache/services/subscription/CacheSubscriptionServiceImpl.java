package com.bhf.aeroncache.services.subscription;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import com.bhf.aeroncache.models.requests.CacheUnsubscribeRequestDetails;
import com.bhf.aeroncache.models.results.*;
import io.aeron.cluster.service.ClientSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.*;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Log4j2
public class CacheSubscriptionServiceImpl<I extends Reusable, K extends Reusable, V extends Reusable> implements CacheSubscriptionService<I,K,V> {

    private final IdleStrategy idleStrategy;
    private final CacheSubscriptionResult<I> subscriptionResult;
    private final CacheUnsubscribeResult<I> unsubscribeResult;
    private final Supplier<I> indexSupplier;

    private final Map<I, Set<ClientSession>> cacheIdToClientSessions = new HashMap<>();

    @Override
    public CacheSubscriptionResult<I> subscribe(CacheSubscriptionRequestDetails<I> requestDetails,
                                                ClientSession session) {
        Set<ClientSession> existingSubscribers;
        if (cacheIdToClientSessions.containsKey(requestDetails.getCacheId())) {
            existingSubscribers = cacheIdToClientSessions.get(requestDetails.getCacheId());
        } else {
            existingSubscribers = new HashSet<>();
            var key = indexSupplier.get();
            key.copyFrom(requestDetails.getCacheId());
            cacheIdToClientSessions.put(key, existingSubscribers);
        }

        subscriptionResult.clear();
        subscriptionResult.setRequestId(requestDetails.getRequestId());
        subscriptionResult.getCacheId().copyFrom(requestDetails.getCacheId());
        if (!existingSubscribers.contains(session)) {
            existingSubscribers.add(session);
            subscriptionResult.setStatus(com.bhf.aeroncache.messages.CacheOperationStatus.SUCCESS);
        } else {
            subscriptionResult.setStatus(com.bhf.aeroncache.messages.CacheOperationStatus.DUPLICATE_SUBSCRIPTION);
        }

        log.debug("Total subscriptions: {}", existingSubscribers.size());

        return subscriptionResult;
    }

    @Override
    public CacheUnsubscribeResult<I> unsubscribe(CacheUnsubscribeRequestDetails<I> requestDetails,
                                                 ClientSession session) {
        unsubscribeResult.clear();
        unsubscribeResult.setRequestId(requestDetails.getRequestId());
        var cacheId = requestDetails.getCacheId();
        unsubscribeResult.getCacheId().copyFrom(cacheId);

        if (cacheIdToClientSessions.containsKey(cacheId)) {
            if (cacheIdToClientSessions.get(cacheId).remove(session)) {
                log.info("Unsubscribed on cache {}, session {}", cacheId, session.id());
                unsubscribeResult.setStatus(com.bhf.aeroncache.messages.CacheOperationStatus.SUCCESS);
            } else {
                unsubscribeResult.setStatus(com.bhf.aeroncache.messages.CacheOperationStatus.UNKNOWN_SUBSCRIPTION);
            }
        } else {
            unsubscribeResult.setStatus(com.bhf.aeroncache.messages.CacheOperationStatus.UNKNOWN_CACHE);
        }

        return unsubscribeResult;
    }

    void sendMessage(final ClientSession session, MutableDirectBuffer msgBuffer, int len) {
        while (session.offer(msgBuffer, 0, len) < 0) {
            idleStrategy.idle();
        }
    }

    private Set<ClientSession> getSessionsForCache(I cacheId) {
        return cacheIdToClientSessions.getOrDefault(cacheId, Collections.emptySet());
    }

    @Override
    public void handleDeleteCache(DeleteCacheResult<I> requestDetails, MutableDirectBuffer egressBuffer,
                                  int length,
                                  long excludeSessionId) {
        for (var session : getSessionsForCache(requestDetails.getCacheId())) {
            if (session.id() != excludeSessionId) {
                log.debug("Sending delete cache update to session: {}", session.id());
                sendMessage(session, egressBuffer, length);
            }
        }
    }

    @Override
    public void handleClearCache(ClearCacheResult<I> clearCacheResult, MutableDirectBuffer egressBuffer,
                                 int length,
                                 long excludeSessionId) {
        for (var session : getSessionsForCache(clearCacheResult.getCacheId())) {
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
        for (var session : getSessionsForCache(removeCacheEntryResult.getCacheId())) {
            if (session.id() != excludeSessionId) {
                log.debug("Sending entry removed update to session: {}", session.id());
                sendMessage(session, egressBuffer, length);
            }
        }
    }

    @Override
    public void handleEntryAdded(AddCacheEntryResult<I, K> addCacheEntryResult,
                                 MutableDirectBuffer egressBuffer, K key,
                                 V value, int length) {
        log.info("Sending entry added to subscribers on cacheId {}", addCacheEntryResult.getCacheId());
        for (var session : getSessionsForCache(addCacheEntryResult.getCacheId())) {
            log.debug("Sending entry added update to session: {}", session.id());
            sendMessage(session, egressBuffer, length);
        }
    }

    @Override
    public void onSessionClose(ClientSession session) {
        log.info("Handling client session closed, sessionId: {}", session.id());
        cacheIdToClientSessions.forEach((cacheId, clientSessions) -> {
            if (clientSessions.remove(session)) {
                log.info("Removed subscription for cache Id: {}, sessionId: {}", cacheId, session.id());
            }
        });
    }
}
