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
    private static final int KEEPALIVE_INTERVAL = 200;
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

    static long lastKeepAlive = 0;

    @Override
    public void sendCreateCache(AeronCluster cluster, long cacheId) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, createCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
        log.info("Sent create cache request");
    }

    public static void handleKeepAlive(AeronCluster cluster) {
        long now = System.currentTimeMillis();

        if (now > lastKeepAlive + KEEPALIVE_INTERVAL) {
            cluster.sendKeepAlive();
            lastKeepAlive = now;
        }
    }

    @Override
    public void sendCreateCacheBlocking(AeronCluster cluster, long cacheId) {
        sendCreateCache(cluster, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void addCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key, String value) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).entryValue(value);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, addCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    @Override
    public void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value) {
        addCacheEntryNonBlocking(cluster, cacheId, key, value);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, getCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    @Override
    public void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key) {
        getCacheEntryNonBlocking(cluster, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void clearCacheNonBlocking(AeronCluster cluster, long cacheId) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, clearCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    @Override
    public void clearCacheBlocking(AeronCluster cluster, long cacheId) {
        clearCacheNonBlocking(cluster, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void deleteCacheNonBlocking(AeronCluster cluster, long cacheId) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, deleteCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    @Override
    public void deleteCacheBlocking(AeronCluster cluster, long cacheId) {
        deleteCacheNonBlocking(cluster, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void removeCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, removeCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    @Override
    public void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key) {
        removeCacheEntryNonBlocking(cluster, cacheId, key);
        waitForResult(cluster);
    }

    /**
     * Wait for results back from the cluster.
     *
     * @param cluster The Aeron Cluster.
     */
    private void waitForResult(AeronCluster cluster) {
        pollEgressUntilMessage(this.getIdleStrategy(), cluster);
        handleKeepAlive(cluster);
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
            handleKeepAlive(cluster);
        }
    }
}
