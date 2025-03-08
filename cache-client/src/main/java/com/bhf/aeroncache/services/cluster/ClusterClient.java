package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cluster.impl.ObservingClusterRequestPublisher;
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
public class ClusterClient implements EgressListener {

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

    private final CreateCacheResult<Long> createCacheResult = new CreateCacheResult<>();
    private final AddCacheEntryResult<Long, String> addCacheEntryResult = new AddCacheEntryResult<>();
    private final ClearCacheResult<Long> clearCacheResult = new ClearCacheResult<>();
    private final DeleteCacheResult<Long> deleteCacheResult = new DeleteCacheResult<>();
    private final RemoveCacheEntryResult<Long, String> removeCacheEntryResult = new RemoveCacheEntryResult<>();
    private final GetCacheEntryResult<Long, String, String> getCacheEntryResult = new GetCacheEntryResult<>();

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
        var key = getCacheEntryDecoder.key();
        var value = getCacheEntryDecoder.value();
        log.info("Got cache entry result from cache {} with key {}, value: {}", cacheID, key, value);
        getCacheEntryResult.setCacheId(cacheID);
        getCacheEntryResult.setEntryKey(key);
        getCacheEntryResult.setEntryValue(value);

        if (cacheResultsCallbacks != null) {
            cacheResultsCallbacks.handleCacheEntryResult(getCacheEntryResult);
        }
    }

    /**
     * Handle a cache created event by decoding it and delegating the result to the consumer.
     *
     * @param buffer The buffer to decode from.
     * @param offset The offset at which to start decoding.
     */
    private void handleCacheCreated(DirectBuffer buffer, int offset) {
        cacheCreatedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheCreatedDecoder.cacheId();
        log.info("Created cache {}", cacheId);
        createCacheResult.clear();
        createCacheResult.setCacheId(cacheId);

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
        String key = addCacheEntryDecoder.key();
        log.info("Got cache entry created message for cache {} with key {}", cacheId, key);
        addCacheEntryResult.clear();
        addCacheEntryResult.setEntryAdded(true);
        addCacheEntryResult.setEntryKey(key);
        addCacheEntryResult.setCacheID(cacheId);

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
        log.info("Got cache entry removed for cache {} with key {}", cacheId, key);
        removeCacheEntryResult.clear();
        removeCacheEntryResult.setKey(key);
        removeCacheEntryResult.setCacheId(cacheId);

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
        log.info("Got cache cleared on cache {}", cacheId);
        clearCacheResult.clear();
        clearCacheResult.setCacheId(cacheId);

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
        log.info("Got cache deleted on cache {}", cacheId);
        deleteCacheResult.clear();
        deleteCacheResult.setCacheId(cacheId);

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
