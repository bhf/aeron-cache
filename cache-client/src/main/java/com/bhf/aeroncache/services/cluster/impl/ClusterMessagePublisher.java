package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.services.cluster.ClusterRequestPublisher;
import io.aeron.cluster.client.AeronCluster;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

/**
 * A basic message publisher with no duty cycle. Simply
 * polls on the egress based on the method being called by the user.
 */
@Setter
@Log4j2
public class ClusterMessagePublisher implements ClusterRequestPublisher {

    private final MutableDirectBuffer msgBuffer = new ExpandableArrayBuffer();

    @Getter
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy();

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    private final AddCacheEntryEncoder addCacheEntryEncoder = new AddCacheEntryEncoder();
    private final GetCacheEntryEncoder getCacheEntryEncoder = new GetCacheEntryEncoder();
    private final ClearCacheEncoder clearCacheEncoder = new ClearCacheEncoder();
    private final DeleteCacheEncoder deleteCacheEncoder = new DeleteCacheEncoder();
    private final RemoveCacheEntryEncoder removeCacheEntryEncoder = new RemoveCacheEntryEncoder();

    @Override
    public void sendCreateCacheBlocking(AeronCluster cluster, String requestId, long cacheId) {
        sendCreateCache(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCreateCache(AeronCluster cluster, String requestId, long cacheId) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishCreateCache(cluster, createCacheEncoder, headerEncoder);
        log.info("Sent create cache request");
    }

    public void publishCreateCache(AeronCluster cluster, CreateCacheEncoder createCacheEncoder, MessageHeaderEncoder headerEncoder){
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, createCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void addCacheEntryBlocking(AeronCluster cluster, String requestId, long cacheId, String key, String value) {
        addCacheEntry(cluster, requestId, cacheId, key, value);
        waitForResult(cluster);
    }

    @Override
    public void addCacheEntry(AeronCluster cluster, String requestId, long cacheId, String key, String value) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).entryValue(value).requestId(requestId);
        publishAddCachEntry(cluster, addCacheEntryEncoder, headerEncoder);
    }

    /**
     * Publish the request to add a cache entry to the cluster.
     * @param cluster The cluster to publish too.
     * @param addCacheEntry The add cache entry message encoded.
     * @param header The message header.
     */
    void publishAddCachEntry(AeronCluster cluster, AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, addCacheEntry.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getCacheEntryBlocking(AeronCluster cluster, String requestId, long cacheId, String key) {
        getCacheEntry(cluster, requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntry(AeronCluster cluster, String requestId, long cacheId, String key) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        publishGetCacheEntry(cluster, getCacheEntryEncoder, headerEncoder);
    }

    /**
     * Publish a request to get an entry from the cluster.
     * @param cluster The cluster to get from.
     * @param getCacheEntry The encoded get entry request.
     * @param header The message header.
     */
    void publishGetCacheEntry(AeronCluster cluster, GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, getCacheEntryEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void clearCacheBlocking(AeronCluster cluster, String requestId, long cacheId) {
        clearCache(cluster, requestId, cacheId);
        waitForResult(cluster);
    }
    @Override
    public void clearCache(AeronCluster cluster, String requestId, long cacheId) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishClearCache(cluster, clearCacheEncoder, headerEncoder);
    }

    /**
     * Publish a request to clear a cache.
     * @param cluster The cluster on which the cache resides.
     * @param clearCache The encoded request to clear a cache.
     * @param header The message header.
     */
    void publishClearCache(AeronCluster cluster, ClearCacheEncoder clearCache, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, clearCache.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void deleteCacheBlocking(AeronCluster cluster, String requestId, long cacheId) {
        deleteCache(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void deleteCache(AeronCluster cluster, String requestId, long cacheId) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishDeleteCache(cluster, deleteCacheEncoder, headerEncoder);
    }

    /**
     * Publish a request to delete an entire cache.
     * @param cluster The cluster on which the cache resides.
     * @param deleteCache The encoded request to delete a cache.
     * @param header The message header.
     */
    void publishDeleteCache(AeronCluster cluster, DeleteCacheEncoder deleteCache, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, deleteCache.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void removeCacheEntryBlocking(AeronCluster cluster, String requestId, long cacheId, String key) {
        removeCacheEntry(cluster, requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void removeCacheEntry(AeronCluster cluster, String requestId, long cacheId, String key) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        publishRemoveCacheEntry(cluster, removeCacheEntryEncoder, headerEncoder);
    }

    /**
     * Publish a request to remove a cache entry.
     * @param cluster The cluster on which the cache resides.
     * @param removeCacheEntry The encoded request to remove a cache entry.
     * @param header The message header.
     */
    void publishRemoveCacheEntry(AeronCluster cluster, RemoveCacheEntryEncoder removeCacheEntry, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, removeCacheEntry.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    /**
     * Wait for results back from the cluster.
     *
     * @param cluster The Aeron Cluster.
     */
    private void waitForResult(AeronCluster cluster) {
        pollEgressUntilMessage(this.getIdleStrategy(), cluster);
    }

    /**
     * Poll the egress of the cluster.
     *
     * @param cluster The cluster to poll.
     * @return Number of fragments processed.
     */
    int pollEgress(AeronCluster cluster) {
        return null == cluster ? 0 : cluster.pollEgress();
    }

    /**
     * Keep polling the egress till we get a message.
     *
     * @param idleStrategy The idle strategy to use.
     * @param cluster      The cluster to poll.
     */
    void pollEgressUntilMessage(IdleStrategy idleStrategy, AeronCluster cluster) {
        idleStrategy.reset();
        while (pollEgress(cluster) <= 0) {
            idleStrategy.idle();
        }
    }
}
