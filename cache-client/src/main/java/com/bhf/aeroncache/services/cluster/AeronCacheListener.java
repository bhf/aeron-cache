package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cluster.impl.ObservingClusterRequestPublisher;
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

import java.util.function.Consumer;


/**
 * Client for connecting to the cluster and executing actions
 * against the cache. Results of actions are handled by a {@link Consumer} for
 * each type of result.
 */
@Setter
@Log4j2
public class AeronCacheListener implements EgressListener {

    @Setter
    private ObservingClusterRequestPublisher cacheResultsCallbacks;

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

    /**
     * {@inheritDoc}
     */
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
        getCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheID = getCacheEntryDecoder.cacheId();
        var status = getCacheEntryDecoder.status();
        var key = getCacheEntryDecoder.key();
        var value = getCacheEntryDecoder.value();
        var requestId = getCacheEntryDecoder.requestId();
        log.info("Got cache entry result from cache {} with key {}, value: {}, requestId: {}, status {}", cacheID, key, value, requestId, status);
        getCacheEntryResult.clear();
        getCacheEntryResult.getCacheId().copyFrom(cacheID);
        getCacheEntryResult.getEntryKey().copyFrom(key);
        getCacheEntryResult.getEntryValue().copyFrom(value);
        getCacheEntryResult.setRequestId(requestId);
        getCacheEntryResult.setStatus(status);

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
        allCacheEntriesResultDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheID = allCacheEntriesResultDecoder.cacheId();
        var status = allCacheEntriesResultDecoder.status();
        var eob = allCacheEntriesResultDecoder.endOfBatch();

        getCacheEntriesResult.clear();
        getCacheEntriesResult.getCacheId().copyFrom(cacheID);
        getCacheEntriesResult.setStatus(status);

        // process group of key-value from the decoder directly into the flyweight

        for (AllCacheEntriesResultDecoder.ItemsDecoder item : allCacheEntriesResultDecoder.items()) {
            var key = new ReusableString();
            var value = new ReusableString();
            key.copyFrom(item.key());
            value.copyFrom(item.value());
            getCacheEntriesResult.getValues().put(key, value);
            log.info("Got key: {}, value: {}", key, value);
        }

        var requestId = allCacheEntriesResultDecoder.requestId();
        getCacheEntriesResult.setRequestId(requestId);
        log.info("Got cache content result from cache {}, requestId: {}, status {}", cacheID, requestId, status);

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleAllCacheEntries(getCacheEntriesResult);
        }
    }

    /**
     * Handle a cache created event by decoding it and delegating the
     * result to the {@link ObservingClusterRequestPublisher}.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheCreated(DirectBuffer buffer, int offset) {
        cacheCreatedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheCreatedDecoder.cacheId();
        var requestId = cacheCreatedDecoder.requestId();
        var status = cacheCreatedDecoder.status();
        log.info("Created cache {}, requestId: {}, status {}", cacheId, requestId, status);
        createCacheResult.clear();
        createCacheResult.getCacheId().copyFrom(cacheId);
        createCacheResult.setRequestId(requestId);
        createCacheResult.setStatus(status);

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
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = addCacheEntryDecoder.cacheId();
        var key = addCacheEntryDecoder.key();
        var requestId = addCacheEntryDecoder.requestId();
        var status = addCacheEntryDecoder.status();
        log.info("Got cache entry created message for cache {} with key {}, requestId: {}, status {}", cacheId, key, requestId, status);
        addCacheEntryResult.clear();
        addCacheEntryResult.setEntryAdded(true);
        addCacheEntryResult.getEntryKey().copyFrom(key);
        addCacheEntryResult.getCacheId().copyFrom(cacheId);
        addCacheEntryResult.setRequestId(requestId);
        addCacheEntryResult.setStatus(status);

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
        cacheEntryRemovedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheEntryRemovedDecoder.cacheId();
        var key = cacheEntryRemovedDecoder.key();
        var requestId = cacheEntryRemovedDecoder.requestId();
        var status = cacheEntryRemovedDecoder.status();
        log.info("Got cache entry removed for cache {} with key {}, requestId: {}, status {}", cacheId, key, requestId, status);
        removeCacheEntryResult.clear();
        removeCacheEntryResult.getKey().copyFrom(key);
        removeCacheEntryResult.getCacheId().copyFrom(cacheId);
        removeCacheEntryResult.setRequestId(requestId);
        removeCacheEntryResult.setStatus(status);

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
        cacheClearedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheClearedDecoder.cacheId();
        var requestId = cacheClearedDecoder.requestId();
        var status = cacheClearedDecoder.status();
        log.info("Got cache cleared on cache {}, requestId: {}, status: {}", cacheId, requestId, status);
        clearCacheResult.clear();
        clearCacheResult.getCacheId().copyFrom(cacheId);
        clearCacheResult.setRequestId(requestId);
        clearCacheResult.setStatus(status);

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
        cacheDeletedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheDeletedDecoder.cacheId();
        var requestId = cacheDeletedDecoder.requestId();
        var status = cacheDeletedDecoder.status();
        log.info("Got cache deleted on cache {}, requestId: {}, status {}", cacheId, requestId, status);
        deleteCacheResult.clear();
        deleteCacheResult.getCacheId().copyFrom(cacheId);
        deleteCacheResult.setRequestId(requestId);
        deleteCacheResult.setStatus(status);

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
        allCacheStatsResultDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var status = allCacheStatsResultDecoder.status();
        cacheStatsResult.clear();
        cacheStatsResult.setOperationStatus(status);

        for (AllCacheStatsResultDecoder.StatsDecoder item : allCacheStatsResultDecoder.stats()) {
            var added = item.added();
            var removed = item.removed();
            var cleared = item.cleared();
            var size = item.size();
            var cacheId = item.cacheId();
            var id = new ReusableLong();
            id.copyFrom(cacheId);
            var stats = new CacheStats<>(id);
            stats.addedCount = added;
            stats.removedCount = removed;
            stats.clearedCount = cleared;
            stats.size = size;
            cacheStatsResult.getStats().add(stats);
            log.info("Got cache: {}, added: {}, removed: {}, cleared: {}, size: {}", cacheId, added, removed, cleared, size);
        }

        var requestId = allCacheStatsResultDecoder.requestId();
        cacheStatsResult.setRequestId(requestId);
        log.info("Got cache stats result, requestId: {}", requestId);

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
        cacheSubscriptionResult.clear();
        cacheSubscriptionResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var cacheId = cacheSubscriptionResponseDecoder.cacheId();
        var status = cacheSubscriptionResponseDecoder.status();
        var requestId = cacheSubscriptionResponseDecoder.requestId();

        log.info("Got cache subscription result on cacheId {}, status {} requestId {}", cacheId, status, requestId);

        cacheSubscriptionResult.getCacheId().copyFrom(cacheId);
        cacheSubscriptionResult.setStatus(status);
        cacheSubscriptionResult.setRequestId(requestId);

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
        cacheUnsubscribeResult.clear();
        cacheUnsubscribeResponseDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var cacheId = cacheUnsubscribeResponseDecoder.cacheId();
        var status = cacheUnsubscribeResponseDecoder.status();
        var requestId = cacheUnsubscribeResponseDecoder.requestId();

        log.info("Got cache unsubscribe result on cacheId {}, status {} requestId {}", cacheId, status, requestId);

        cacheUnsubscribeResult.getCacheId().copyFrom(cacheId);
        cacheUnsubscribeResult.setStatus(status);
        cacheUnsubscribeResult.setRequestId(requestId);

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheUnsubscribeResponse(cacheUnsubscribeResult);
        }
    }

    private void handleCacheEntryUpdated(DirectBuffer buffer, int offset) {
        cacheEntryUpdateResult.clear();
        cacheEntryUpdateDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);

        var cacheId = cacheEntryUpdateDecoder.cacheId();
        var key = cacheEntryUpdateDecoder.key();
        var value = cacheEntryUpdateDecoder.value();
        var requestId = cacheEntryUpdateDecoder.requestId();

        log.info("Got cache entry updated on cacheId {}, key {} requestId {}", cacheId, key, requestId);

        cacheEntryUpdateResult.getCacheId().copyFrom(cacheId);
        cacheEntryUpdateResult.setRequestId(requestId);
        cacheEntryUpdateResult.getKey().copyFrom(key);
        cacheEntryUpdateResult.getValue().copyFrom(value);

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
