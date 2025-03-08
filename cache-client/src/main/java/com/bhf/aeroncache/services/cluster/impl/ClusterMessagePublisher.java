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
    public void sendCreateCacheBlocking(AeronCluster cluster, long cacheId) {
        sendCreateCache(cluster, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCreateCache(AeronCluster cluster, long cacheId) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
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
    public void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value) {
        addCacheEntry(cluster, cacheId, key, value);
        waitForResult(cluster);
    }

    @Override
    public void addCacheEntry(AeronCluster cluster, long cacheId, String key, String value) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).entryValue(value);
        publishAddCachEntry(cluster, addCacheEntryEncoder, headerEncoder);
    }

    void publishAddCachEntry(AeronCluster cluster, AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, addCacheEntry.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key) {
        getCacheEntry(cluster, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntry(AeronCluster cluster, long cacheId, String key) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key);
        publishGetCacheEntry(cluster, getCacheEntryEncoder, headerEncoder);
    }

    void publishGetCacheEntry(AeronCluster cluster, GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, getCacheEntryEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void clearCacheBlocking(AeronCluster cluster, long cacheId) {
        clearCache(cluster, cacheId);
        waitForResult(cluster);
    }
    @Override
    public void clearCache(AeronCluster cluster, long cacheId) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        publishClearCache(cluster, clearCacheEncoder, headerEncoder);
    }

    void publishClearCache(AeronCluster cluster, ClearCacheEncoder clearCache, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, clearCache.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void deleteCacheBlocking(AeronCluster cluster, long cacheId) {
        deleteCache(cluster, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void deleteCache(AeronCluster cluster, long cacheId) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        publishDeleteCache(cluster, deleteCacheEncoder, headerEncoder);
    }

    void publishDeleteCache(AeronCluster cluster, DeleteCacheEncoder deleteCache, MessageHeaderEncoder header) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, deleteCache.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key) {
        removeCacheEntry(cluster, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void removeCacheEntry(AeronCluster cluster, long cacheId, String key) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key);
        publishRemoveCacheEntry(cluster, removeCacheEntryEncoder, headerEncoder);
    }

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
