package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.encoders.CacheResponseDecoder;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import lombok.Getter;
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
public class AeronCacheClusterListener implements EgressListener {

    @Setter
    private CacheResponseHandler cacheResultsCallbacks;

    @Getter
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy();

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    private final CacheCreatedDecoder cacheCreatedDecoder = new CacheCreatedDecoder();
    private final CacheEntryCreatedDecoder addCacheEntryDecoder = new CacheEntryCreatedDecoder();
    private final CacheEntryResultDecoder getCacheEntryDecoder = new CacheEntryResultDecoder();
    private final CacheClearedDecoder cacheClearedDecoder = new CacheClearedDecoder();
    private final CacheDeletedDecoder cacheDeletedDecoder = new CacheDeletedDecoder();
    private final CacheEntryRemovedDecoder cacheEntryRemovedDecoder = new CacheEntryRemovedDecoder();
    private final AllCacheEntriesResultDecoder allCacheEntriesResultDecoder = new AllCacheEntriesResultDecoder();
    private final AllCacheStatsResultDecoder allCacheStatsResultDecoder = new AllCacheStatsResultDecoder();
    private final CacheSubscriptionResponseDecoder cacheSubscriptionResponseDecoder = new CacheSubscriptionResponseDecoder();
    private final CacheUnsubscribeResponseDecoder cacheUnsubscribeResponseDecoder = new CacheUnsubscribeResponseDecoder();
    private final CacheEntryUpdateDecoder cacheEntryUpdateDecoder = new CacheEntryUpdateDecoder();

    private final CreateCacheResult<ReusableLong> createCacheResult = new CreateCacheResult<>(SupplierUtils.longSupplier.get());
    private final AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult = new AddCacheEntryResult<>(SupplierUtils.longSupplier.get(), SupplierUtils.stringSupplier.get());
    private final ClearCacheResult<ReusableLong> clearCacheResult = new ClearCacheResult<>(SupplierUtils.longSupplier.get());
    private final DeleteCacheResult<ReusableLong> deleteCacheResult = new DeleteCacheResult<>(SupplierUtils.longSupplier.get());
    private final RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult = new RemoveCacheEntryResult<>(SupplierUtils.longSupplier.get(), SupplierUtils.stringSupplier.get());
    private final GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult = new GetCacheEntryResult<>(SupplierUtils.longSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
    private final GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getCacheEntriesResult = new GetAllCacheEntriesResult<>(SupplierUtils.longSupplier.get());
    private final CacheStatsResult<ReusableLong> cacheStatsResult = new CacheStatsResult<>();
    private final CacheSubscriptionResult<ReusableLong> cacheSubscriptionResult = new CacheSubscriptionResult<>(SupplierUtils.longSupplier.get());
    private final CacheUnsubscribeResult<ReusableLong> cacheUnsubscribeResult = new CacheUnsubscribeResult<>(SupplierUtils.longSupplier.get());
    private final CacheEntryUpdateResult<ReusableLong, ReusableString, ReusableString> cacheEntryUpdateResult = new CacheEntryUpdateResult<>(SupplierUtils.longSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());

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

        log.info("Got client side message with TID {}", templateId);

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
        CacheResponseDecoder.decodeGetCacheEntryResult(getCacheEntryDecoder, headerDecoder, getCacheEntryResult, buffer, offset);
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
        CacheResponseDecoder.decodeAllCacheEntriesResult(allCacheEntriesResultDecoder, headerDecoder, getCacheEntriesResult, buffer, offset);
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
        CacheResponseDecoder.decodeCacheCreated(createCacheResult, cacheCreatedDecoder, headerDecoder, buffer, offset);
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
        CacheResponseDecoder.decodeAddCacheEntryResult(addCacheEntryDecoder, headerDecoder, addCacheEntryResult, buffer, offset);
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
        CacheResponseDecoder.decodeCacheEntryRemoved(cacheEntryRemovedDecoder, headerDecoder, removeCacheEntryResult, buffer, offset);
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
        CacheResponseDecoder.decodeCacheCleared(cacheClearedDecoder, headerDecoder, clearCacheResult, buffer, offset);
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
        CacheResponseDecoder.decodeCacheDeleted(cacheDeletedDecoder, headerDecoder, deleteCacheResult, buffer, offset);
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
        CacheResponseDecoder.decodeAllCacheStatsResult(allCacheStatsResultDecoder, headerDecoder, cacheStatsResult, buffer, offset);
        log.info("Got cache stats result, requestId: {}", cacheStatsResult.getRequestId());

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
        CacheResponseDecoder.decodeCacheSubscribeResult(cacheSubscriptionResponseDecoder, headerDecoder, cacheSubscriptionResult, buffer, offset);
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
        CacheResponseDecoder.decodeCacheUnsubscribeResult(cacheUnsubscribeResponseDecoder, headerDecoder, cacheUnsubscribeResult, buffer, offset);
        log.info("Got cache unsubscribe result on cacheId {}, status {} requestId {}",
                cacheUnsubscribeResult.getCacheId(), cacheUnsubscribeResult.getStatus(), cacheUnsubscribeResult.getRequestId());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
        }
    }

    private void handleCacheEntryUpdated(DirectBuffer buffer, int offset) {
        CacheResponseDecoder.decodeCacheEntryUpdated(cacheEntryUpdateDecoder, headerDecoder, cacheEntryUpdateResult, buffer, offset);
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
    }

}
