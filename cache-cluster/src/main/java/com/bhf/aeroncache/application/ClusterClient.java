package com.bhf.aeroncache.application;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Consumer;


/**
 * Client for connecting to the cluster and executing actions
 * against the cache. Results of actions are handled by a {@link Consumer} for
 * each type of result.
 */
@Log4j2
@Setter
public class ClusterClient implements EgressListener {
    private final MutableDirectBuffer msgBuffer = new ExpandableArrayBuffer();
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy();

    final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    final CacheCreatedDecoder cacheCreatedDecoder = new CacheCreatedDecoder();
    final AddCacheEntryEncoder addCacheEntryEncoder = new AddCacheEntryEncoder();
    final ClearCacheEncoder clearCacheEncoder = new ClearCacheEncoder();
    final DeleteCacheEncoder deleteCacheEncoder = new DeleteCacheEncoder();
    final RemoveCacheEntryEncoder removeCacheEntryEncoder = new RemoveCacheEntryEncoder();
    final AddCacheEntryDecoder addCacheEntryDecoder = new AddCacheEntryDecoder();
    final CacheEntryRemovedDecoder cacheEntryRemovedDecoder = new CacheEntryRemovedDecoder();
    final CacheClearedDecoder cacheClearedDecoder=new CacheClearedDecoder();

    final CreateCacheResult<Long> createCacheResult = new CreateCacheResult<>();
    final AddCacheEntryResult<Long, String> addCacheEntryResult = new AddCacheEntryResult<>();
    final ClearCacheResult<Long> clearCacheResult = new ClearCacheResult<>();
    final DeleteCacheResult<Long> deleteCacheResult = new DeleteCacheResult<>();
    final RemoveCacheEntryResult<Long,String> removeCacheEntryResult = new RemoveCacheEntryResult<>();

    Consumer<CreateCacheResult<Long>> createCacheConsumer;
    Consumer<AddCacheEntryResult<Long, String>> addCacheEntryConsumer;
    Consumer<ClearCacheResult<Long>> clearCacheConsumer;
    Consumer<DeleteCacheResult<Long>> deleteCacheConsumer;
    Consumer<RemoveCacheEntryResult<Long, String>> removeCacheEntryConsumer;

    /**
     * {@inheritDoc}
     */
    public void onMessage(
            final long clusterSessionId,
            final long timestamp,
            final DirectBuffer buffer,
            final int offset,
            final int length,
            final Header header) {
        headerDecoder.wrap(buffer, offset);
        final int templateId = headerDecoder.templateId();

        switch (templateId) {
            case CacheCreatedDecoder.TEMPLATE_ID -> handleCacheCreated(buffer, offset);
            case CacheEntryCreatedDecoder.TEMPLATE_ID -> handleCacheEntryCreated(buffer, offset);
            case CacheClearedDecoder.TEMPLATE_ID -> handleCacheCleared(buffer, offset);
            case CacheDeletedDecoder.TEMPLATE_ID -> handleCacheDeleted(buffer, offset);
            case CacheEntryRemovedDecoder.TEMPLATE_ID -> handleCacheEntryRemoved(buffer, offset);
            default -> log.warn("Got unknown message with TID {}", templateId);
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
        log.info("Created cache " + cacheId);
        createCacheResult.clear();
        createCacheResult.setCacheId(cacheId);
        createCacheConsumer.accept(createCacheResult);
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
        log.debug("Got cache entry created message for cache {}, key {}", cacheId, key);
        addCacheEntryResult.clear();
        addCacheEntryResult.setEntryAdded(true);
        addCacheEntryResult.setEntryKey(key);
        addCacheEntryResult.setCacheID(cacheId);
        addCacheEntryConsumer.accept(addCacheEntryResult);
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
        log.debug("Got cache entry removed for cache {}, key {}", cacheId, key);
        removeCacheEntryResult.clear();
        removeCacheEntryResult.setKey(key);
        removeCacheEntryResult.setCacheId(cacheId);
        removeCacheEntryConsumer.accept(removeCacheEntryResult);
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
        log.debug("Got cache cleared on cache {}", cacheId);
        clearCacheResult.clear();
        clearCacheResult.setCacheId(cacheId);
        clearCacheConsumer.accept(clearCacheResult);
    }

    /**
     * Handle a cache being deleted by decoding it and delegating the result to the consumer.
     *
     * @param buffer The buffer to decode from.
     * @param offset THe offset at which to start decoding.
     */
    private void handleCacheDeleted(DirectBuffer buffer, int offset) {
        CacheDeletedDecoder cacheDeletedDecoder=new CacheDeletedDecoder();
        cacheDeletedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheDeletedDecoder.cacheId();
        log.debug("Got cache deleted on cache {}", cacheId);
        deleteCacheResult.clear();
        deleteCacheResult.setCacheId(cacheId);
        deleteCacheConsumer.accept(deleteCacheResult);
    }

    /**
     * Send a message to create a cache instance.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache to create.
     */
    public void sendCreateCache(AeronCluster cluster, long cacheId) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, createCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    /**
     * Send a message to add a cache entry.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheID The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param value   The value to use.
     */
    public void sendAddCacheEntry(AeronCluster cluster, long cacheID, String key, String value) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheID).key(key).entryValue(value);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, addCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    /**
     * Send a message to clear a cache.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're clearing out.
     */
    public void sendClearCache(AeronCluster cluster, long cacheId) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, clearCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    /**
     * Send a message to delete a cache.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    public void sendDeleteCache(AeronCluster cluster, long cacheId) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, deleteCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    /**
     * Send a message to remove a cache entry.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     */
    public void removeCacheEntry(AeronCluster cluster, long cacheId, String key) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, removeCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onSessionEvent(
            final long correlationId,
            final long clusterSessionId,
            final long leadershipTermId,
            final int leaderMemberId,
            final EventCode code,
            final String detail) {
        log.debug(
                "Got session event with correlationId {}, cluster session ID {}," +
                        " leader term ID {}, leader member ID {}, event code {}, details {}",
                correlationId, clusterSessionId, leadershipTermId, leaderMemberId,
                code, detail);
    }

    /**
     * {@inheritDoc}
     */
    public void onNewLeader(
            final long clusterSessionId,
            final long leadershipTermId,
            final int leaderMemberId,
            final String ingressEndpoints) {
        log.debug("Got new cluster leader, leaderID {}, leader term Id {}, " +
                        "cluster session ID {}, ingress endpoints {}", leaderMemberId, leadershipTermId,
                clusterSessionId, ingressEndpoints);
    }

}
