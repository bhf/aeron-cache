package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.codecs.CacheResponseDecoder;
import com.bhf.aeroncache.handlers.ClusterSessionEventHandler;
import com.bhf.aeroncache.handlers.NoOpClusterSessionEventHandler;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

/**
 * Decode SBE messages related to cache requests and pass the result
 * {@link com.bhf.aeroncache.annotations.Flyweight} to the {@link CacheResponseHandler}.
 */
@Setter
@Log4j2
@RequiredArgsConstructor
public class AeronCacheClusterListener implements EgressListener {

    @Setter
    private CacheResponseHandler cacheResultsCallbacks;

    @Getter
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy();

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    private final CacheResponseDecoder cacheResponseDecoder;

    private final CreateCacheResult<ReusableString> createCacheResult = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
    private final AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult = new AddCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
    private final ClearCacheResult<ReusableString> clearCacheResult = new ClearCacheResult<>(SupplierUtils.stringSupplier.get());
    private final DeleteCacheResult<ReusableString> deleteCacheResult = new DeleteCacheResult<>(SupplierUtils.stringSupplier.get());
    private final RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult = new RemoveCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
    private final GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult = new GetCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
    private final GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getCacheEntriesResult = new GetAllCacheEntriesResult<>(SupplierUtils.stringSupplier.get());
    private final CacheStatsResult<ReusableString> cacheStatsResult = new CacheStatsResult<>();
    private final CacheSubscriptionResult<ReusableString> cacheSubscriptionResult = new CacheSubscriptionResult<>(SupplierUtils.stringSupplier.get());
    private final CacheUnsubscribeResult<ReusableString> cacheUnsubscribeResult = new CacheUnsubscribeResult<>(SupplierUtils.stringSupplier.get());
    private final CacheEntryUpdateResult<ReusableString, ReusableString, ReusableString> cacheEntryUpdateResult = new CacheEntryUpdateResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());

    private final ClusterSessionEventHandler sessionEventHandler = new NoOpClusterSessionEventHandler();

    @Override
    public void onMessage(
            final long clusterSessionId,
            final long timestamp,
            final DirectBuffer buffer,
            final int offset,
            final int length,
            final Header header) {
        headerDecoder.wrap(buffer, offset);
        final int templateId = headerDecoder.templateId();

        log.debug("Got client side message with TID {}", templateId);

        switch (templateId) {
            case CacheCreatedDecoder.TEMPLATE_ID -> handleCacheCreated(buffer, offset);
            case CacheEntryCreatedDecoder.TEMPLATE_ID -> handleCacheEntryCreated(buffer, offset);
            case CacheEntryResultDecoder.TEMPLATE_ID -> handleCacheEntryResult(buffer, offset);
            case CacheClearedDecoder.TEMPLATE_ID -> handleCacheCleared(buffer, offset);
            case CacheDeletedDecoder.TEMPLATE_ID -> handleCacheDeleted(buffer, offset);
            case CacheEntryRemovedDecoder.TEMPLATE_ID -> handleCacheEntryRemoved(buffer, offset);
            case AllCacheEntriesResultDecoder.TEMPLATE_ID -> handleAllCacheEntriesResult(buffer, offset);
            case AllCacheStatsResultDecoder.TEMPLATE_ID -> handleAllCacheStatsResult(buffer, offset);
            case CacheSubscriptionResponseDecoder.TEMPLATE_ID -> handleCacheSubscribeResult(buffer, offset);
            case CacheUnsubscribeResponseDecoder.TEMPLATE_ID -> handleCacheUnsubscribeResult(buffer, offset);
            case CacheEntryUpdateDecoder.TEMPLATE_ID -> handleCacheEntryUpdated(buffer, offset);
            default -> log.warn("Got unknown message with TID {}", templateId);
        }
    }

    /**
     * Handle the result of getting a cache entry.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheEntryResult(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeGetCacheEntryResult(buffer, offset, getCacheEntryResult);
        log.info("Got cache entry result from cache {} with key {}, value: {}, requestId: {}, status {}",
                getCacheEntryResult.getCacheId(), getCacheEntryResult.getEntryKey(), getCacheEntryResult.getEntryValue(),
                getCacheEntryResult.getRequestId(), getCacheEntryResult.getStatus());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheEntryResult(getCacheEntryResult);
        }
    }

    /**
     * Handle the result of getting all cache entries.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleAllCacheEntriesResult(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeAllCacheEntriesResult(buffer, offset, getCacheEntriesResult);
        log.info("Got cache content result from cache {}, requestId: {}, status {}",
                getCacheEntriesResult.getCacheId(), getCacheEntriesResult.getRequestId(), getCacheEntriesResult.getStatus());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleAllCacheEntries(getCacheEntriesResult);
        }
    }

    /**
     * Handle a cache created event by decoding it and delegating the
     * result to the {@link CacheResponseHandler}.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheCreated(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeCacheCreated(buffer, offset, createCacheResult);
        log.info("Created cache {}, requestId: {}, status {}",
                createCacheResult.getCacheId(), createCacheResult.getRequestId(), createCacheResult.getStatus());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheCreated(createCacheResult);
        }
    }

    /**
     * Handle a cache entry being created by decoding it and delegating the result to the consumer.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheEntryCreated(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeAddCacheEntryResult(buffer, offset, addCacheEntryResult);
        log.info("Got cache entry created message for cache {} with key {}, requestId: {}, status {}",
                addCacheEntryResult.getCacheId(), addCacheEntryResult.getEntryKey(),
                addCacheEntryResult.getRequestId(), addCacheEntryResult.getStatus());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheEntryCreated(addCacheEntryResult);
        }
    }

    /**
     * Handle a cache entry being removed by decoding it and delegating the result to the consumer.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheEntryRemoved(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeCacheEntryRemoved(buffer, offset, removeCacheEntryResult);
        log.info("Got cache entry removed for cache {} with key {}, requestId: {}, status {}",
                removeCacheEntryResult.getCacheId(), removeCacheEntryResult.getKey(),
                removeCacheEntryResult.getRequestId(), removeCacheEntryResult.getStatus());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheEntryRemoved(removeCacheEntryResult);
        }
    }

    /**
     * Handle a cache being cleared by decoding it and delegating the result to the consumer.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheCleared(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeCacheCleared(buffer, offset, clearCacheResult);
        log.info("Got cache cleared on cache {}, requestId: {}, status: {}",
                clearCacheResult.getCacheId(), clearCacheResult.getRequestId(), clearCacheResult.getStatus());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheCleared(clearCacheResult);
        }
    }

    /**
     * Handle a cache being deleted by decoding it and delegating the result to the consumer.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheDeleted(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeCacheDeleted(buffer, offset, deleteCacheResult);
        log.info("Got cache deleted on cache {}, requestId: {}, status {}",
                deleteCacheResult.getCacheId(), deleteCacheResult.getRequestId(), deleteCacheResult.getStatus());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheDeleted(deleteCacheResult);
        }
    }

    /**
     * Handle the result of getting all cache stats.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleAllCacheStatsResult(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeAllCacheStatsResult(buffer, offset, cacheStatsResult);
        log.debug("Got cache stats result, requestId: {}", cacheStatsResult.getRequestId());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleAllCacheStats(cacheStatsResult);
        }
    }

    /**
     * Handle the result of a subscription request.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheSubscribeResult(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeCacheSubscribeResult(buffer, offset, cacheSubscriptionResult);
        log.info("Got cache subscription result on cacheId {}, status {} requestId {}",
                cacheSubscriptionResult.getCacheId(), cacheSubscriptionResult.getStatus(), cacheSubscriptionResult.getRequestId());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheSubscribeResponse(cacheSubscriptionResult);
        }
    }

    /**
     * Handle the result of an unsubscribe request.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheUnsubscribeResult(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeCacheUnsubscribeResult(cacheUnsubscribeResult, buffer, offset);
        log.info("Got cache unsubscribe result on cacheId {}, status {} requestId {}",
                cacheUnsubscribeResult.getCacheId(), cacheUnsubscribeResult.getStatus(), cacheUnsubscribeResult.getRequestId());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
        }
    }

    private void handleCacheEntryUpdated(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeCacheEntryUpdated(cacheEntryUpdateResult, buffer, offset);
        log.info("Got cache entry updated on cacheId {}, key {} requestId {}",
                cacheEntryUpdateResult.getCacheId(), cacheEntryUpdateResult.getKey(), cacheEntryUpdateResult.getRequestId());
        
        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheEntryUpdated(cacheEntryUpdateResult);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSessionEvent(
            final long correlationId,
            final long clusterSessionId,
            final long leadershipTermId,
            final int leaderMemberId,
            final EventCode code,
            final String detail) {
        log.info(
                "Got session event with correlationId " + correlationId + ", cluster session ID " + clusterSessionId +
                        " leader term ID " + leadershipTermId + ", leader member ID " + leaderMemberId + ", event code " + code + ", details " + detail);
        sessionEventHandler.handleSessionEvent(correlationId, clusterSessionId, leadershipTermId, leaderMemberId, code, detail);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewLeader(
            final long clusterSessionId,
            final long leadershipTermId,
            final int leaderMemberId,
            final String ingressEndpoints) {
        log.info("Got new cluster leader, leaderID " + leaderMemberId + ", leader term Id " + leadershipTermId + ", " +
                "cluster session ID " + clusterSessionId + ", ingress endpoints " + ingressEndpoints);
        sessionEventHandler.handleNewLeader(clusterSessionId, leadershipTermId, leaderMemberId, ingressEndpoints);
    }

}
