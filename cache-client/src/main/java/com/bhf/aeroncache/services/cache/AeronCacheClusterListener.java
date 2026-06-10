package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.handlers.ClusterSessionEventHandler;
import com.bhf.aeroncache.handlers.NoOpClusterSessionEventHandler;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cacheclient.CacheClientSchemDetailsProvider;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Supplier;

/**
 * Decode SBE messages related to cache requests and pass the result
 * {@link com.bhf.aeroncache.annotations.Flyweight} to the {@link CacheResponseHandler}.
 */
@Setter
@Log4j2
public class AeronCacheClusterListener<I extends Reusable, K extends Reusable, V extends Reusable> implements EgressListener {

    @Setter
    private CacheResponseHandler<I,K,V> cacheResultsCallbacks;

    @Getter
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy();

    private final CacheResponseDecoder<I,K,V> cacheResponseDecoder;
    private final CacheClientSchemDetailsProvider schemaDetails;

    private final CreateCacheResult<I> createCacheResult;
    private final AddCacheEntryResult<I, K> addCacheEntryResult;
    private final ClearCacheResult<I> clearCacheResult;
    private final DeleteCacheResult<I> deleteCacheResult;
    private final RemoveCacheEntryResult<I, K> removeCacheEntryResult;
    private final GetCacheEntryResult<I, K, V> getCacheEntryResult;
    private final GetAllCacheEntriesResult<I, K, V> getCacheEntriesResult;
    private final CacheSubscriptionResult<I,K,V> cacheSubscriptionResult;
    private final CacheUnsubscribeResult<I> cacheUnsubscribeResult;
    private final CacheEntryUpdateResult<I, K, V> cacheEntryUpdateResult;
    private final BulkCacheOpsResult<I,K,V> bulkCacheOpsResult;
    private final ClusterSessionEventHandler sessionEventHandler = new NoOpClusterSessionEventHandler();
    private final CacheStatsResult<I> cacheStatsResult = new CacheStatsResult<>();

    public AeronCacheClusterListener(CacheResponseDecoder<I,K,V> cacheResponseDecoder, CacheClientSchemDetailsProvider schemaDetails, Supplier<I> indexSupplier, Supplier<K> keySupplier, Supplier<V> valueSupplier) {
        this.cacheResponseDecoder = cacheResponseDecoder;
        this.schemaDetails = schemaDetails;
        createCacheResult = new CreateCacheResult<>(indexSupplier.get());
        addCacheEntryResult = new AddCacheEntryResult<>(indexSupplier.get(), keySupplier.get());
        clearCacheResult = new ClearCacheResult<>(indexSupplier.get());
        deleteCacheResult = new DeleteCacheResult<>(indexSupplier.get());
        removeCacheEntryResult = new RemoveCacheEntryResult<>(indexSupplier.get(), keySupplier.get());
        getCacheEntryResult = new GetCacheEntryResult<>(indexSupplier.get(), keySupplier.get(), valueSupplier.get());
        getCacheEntriesResult = new GetAllCacheEntriesResult<>(indexSupplier.get());
        cacheSubscriptionResult = new CacheSubscriptionResult<>(indexSupplier.get());
        cacheUnsubscribeResult = new CacheUnsubscribeResult<>(indexSupplier.get());
        cacheEntryUpdateResult = new CacheEntryUpdateResult<>(indexSupplier.get(), keySupplier.get(), valueSupplier.get());
        bulkCacheOpsResult = new BulkCacheOpsResult<>(indexSupplier, keySupplier, valueSupplier);
    }

    @Override
    public void onMessage(
            final long clusterSessionId,
            final long timestamp,
            final DirectBuffer buffer,
            final int offset,
            final int length,
            final Header header) {

        final int templateId = (buffer.getShort(offset + 2, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);

        log.debug("Got client side message with TID {}", templateId);

        if (templateId == schemaDetails.getCacheCreatedId()) {
            handleCacheCreated(buffer, offset);
        } else if (templateId == schemaDetails.getCacheEntryCreatedId()) {
            handleCacheEntryCreated(buffer, offset);
        } else if (templateId == schemaDetails.getCacheEntryResultId()) {
            handleCacheEntryResult(buffer, offset);
        } else if (templateId == schemaDetails.getCacheClearedId()) {
            handleCacheCleared(buffer, offset);
        } else if (templateId == schemaDetails.getCacheDeletedId()) {
            handleCacheDeleted(buffer, offset);
        } else if (templateId == schemaDetails.getCacheEntryRemovedId()) {
            handleCacheEntryRemoved(buffer, offset);
        } else if (templateId == schemaDetails.getAllCacheEntriesResultId()) {
            handleAllCacheEntriesResult(buffer, offset);
        } else if (templateId == schemaDetails.getAllCacheStatsResultId()) {
            handleAllCacheStatsResult(buffer, offset);
        } else if (templateId == schemaDetails.getCacheSubscriptionResponseId()) {
            handleCacheSubscribeResult(buffer, offset);
        } else if (templateId == schemaDetails.getCacheUnsubscribeResponseId()) {
            handleCacheUnsubscribeResult(buffer, offset);
        } else if (templateId == schemaDetails.getCacheEntryUpdateId()) {
            handleCacheEntryUpdated(buffer, offset);
        } else if (templateId == schemaDetails.bulkOperationsResponseId()) {
            handleBulkOperationResponse(buffer, offset);
        } else {
            log.warn("Got unknown message with TID {}", templateId);
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
        cacheResponseDecoder.decodeCacheUnsubscribeResult(buffer, offset, cacheUnsubscribeResult);
        log.info("Got cache unsubscribe result on cacheId {}, status {} requestId {}",
                cacheUnsubscribeResult.getCacheId(), cacheUnsubscribeResult.getStatus(), cacheUnsubscribeResult.getRequestId());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
        }
    }

    /**
     * Handle the result of a cache entry being added.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheEntryUpdated(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeCacheEntryUpdated(buffer, offset, cacheEntryUpdateResult);
        log.info("Got cache entry updated on cacheId {}, key {} requestId {}",
                cacheEntryUpdateResult.getCacheId(), cacheEntryUpdateResult.getKey(), cacheEntryUpdateResult.getRequestId());
        
        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheEntryUpdated(cacheEntryUpdateResult);
        }
    }

    /**
     * Handle the result of a bulk operation done on the cache.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleBulkOperationResponse(DirectBuffer buffer, int offset) {
        cacheResponseDecoder.decodeBulkCacheOpsResult(buffer, offset, bulkCacheOpsResult);
        log.info("Got bulk ops results from cache on request Id {}", bulkCacheOpsResult.getRequestId());

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleBulkOperationsResult(bulkCacheOpsResult);
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
