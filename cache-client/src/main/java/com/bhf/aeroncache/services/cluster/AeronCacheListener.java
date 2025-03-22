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

    private final CreateCacheResult<ReusableLong> createCacheResult = new CreateCacheResult<>(SupplierUtils.longSupplier.get());
    private final AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult = new AddCacheEntryResult<>(SupplierUtils.longSupplier.get(), SupplierUtils.stringSupplier.get());
    private final ClearCacheResult<ReusableLong> clearCacheResult = new ClearCacheResult<>(SupplierUtils.longSupplier.get());
    private final DeleteCacheResult<ReusableLong> deleteCacheResult = new DeleteCacheResult<>(SupplierUtils.longSupplier.get());
    private final RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult = new RemoveCacheEntryResult<>(SupplierUtils.longSupplier.get(), SupplierUtils.stringSupplier.get());
    private final GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult = new GetCacheEntryResult<>(SupplierUtils.longSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());

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
     * @param offset THe offset at which to start decoding.
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
        addCacheEntryResult.getCacheID().copyFrom(cacheId);
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
     * @param offset THe offset at which to start decoding.
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
     * @param offset THe offset at which to start decoding.
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
     * @param offset THe offset at which to start decoding.
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
