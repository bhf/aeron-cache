package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import io.aeron.cluster.client.AeronCluster;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Consumer;

@Setter
@Log4j2
public class ClusterMessagePublisher {
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

    private final ClusterClient client;

    public ClusterMessagePublisher(ClusterClient client) {
        this.client = client;
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
        handleKeepAlive(cluster);
        log.info("Sent create cache request");
    }

    static long lastKeepAlive = 0;

    public static void handleKeepAlive(AeronCluster cluster) {
        long now = System.currentTimeMillis();

        if (now > lastKeepAlive + KEEPALIVE_INTERVAL) {
            cluster.sendKeepAlive();
            lastKeepAlive = now;
        }
    }

    /**
     * Send a message to create a cache instance, block
     * until you get a response.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache to create.
     */
    public void sendCreateCacheBlocking(AeronCluster cluster, long cacheId) {
        sendCreateCache(cluster, cacheId);
        waitForResult(cluster);
    }

    /**
     * Send a message to create a cache instance, blocking
     * until you get a response. Passes the result to the
     * Consumer.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache to create.
     * @param consumer The consumer of the result.
     */
    public void sendCreateCacheBlocking(AeronCluster cluster, long cacheId, Consumer<CreateCacheResult<Long>> consumer) {
        client.setCreateCacheConsumer(consumer);
        sendCreateCacheBlocking(cluster, cacheId);
        client.setCreateCacheConsumer(null);
    }

    /**
     * Send a message to add a cache entry in a non-blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param value   The value to use.
     */
    public void addCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key, String value) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).entryValue(value);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, addCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    /**
     * Send a message to add a cache entry and block
     * until you get the result back.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param value   The value to use.
     */
    public void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value) {
        addCacheEntryNonBlocking(cluster, cacheId, key, value);
        waitForResult(cluster);
    }

    /**
     * Send a message to add a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param value   The value to use.
     * @param c       The consumer that will handle the result.
     */
    public void addCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, String value, Consumer<AddCacheEntryResult<Long, String>> c) {
        client.setAddCacheEntryConsumer(c);
        addCacheEntryBlocking(cluster, cacheId, key, value);
        client.setAddCacheEntryConsumer(null);
    }

    /**
     * Send a message to get a cache entry.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     */
    public void getCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, getCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    /**
     * Send a message to get a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     * @param c       The consumer to handle the result.
     */
    public void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<GetCacheEntryResult<Long, String, String>> c) {
        client.setGetCacheEntryConsumer(c);
        getCacheEntryBlocking(cluster, cacheId, key);
        client.setGetCacheEntryConsumer(null);
    }

    /**
     * Send a message to get a cache entry synchronously.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're adding too.
     * @param key     The key to use.
     */
    public void getCacheEntryBlocking(AeronCluster cluster, long cacheId, String key) {
        getCacheEntryNonBlocking(cluster, cacheId, key);
        waitForResult(cluster);
    }

    /**
     * Send a message to clear a cache.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're clearing out.
     */
    public void clearCacheNonBlocking(AeronCluster cluster, long cacheId) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, clearCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    /**
     * Send a message to clear a cache. Blocks until it gets a response.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're clearing out.
     */
    public void clearCacheBlocking(AeronCluster cluster, long cacheId) {
        clearCacheNonBlocking(cluster, cacheId);
        waitForResult(cluster);
    }

    /**
     * Send a message to delete a cache.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    public void deleteCacheNonBlocking(AeronCluster cluster, long cacheId) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, deleteCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    /**
     * Send a message to delete a cache in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    public void deleteCacheBlocking(AeronCluster cluster, long cacheId) {
        deleteCacheNonBlocking(cluster, cacheId);
        waitForResult(cluster);
    }

    /**
     * Send a message to delete a cache in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're deleting.
     */
    public void deleteCacheBlocking(AeronCluster cluster, long cacheId, Consumer<DeleteCacheResult<Long>> consumer) {
        client.setDeleteCacheConsumer(consumer);
        deleteCacheBlocking(cluster, cacheId);
        client.setDeleteCacheConsumer(null);
    }

    /**
     * Send a message to remove a cache entry.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     */
    public void removeCacheEntryNonBlocking(AeronCluster cluster, long cacheId, String key) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key);
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, 0, removeCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
        handleKeepAlive(cluster);
    }

    /**
     * Send a message to remove a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     */
    public void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key) {
        removeCacheEntryNonBlocking(cluster, cacheId, key);
        waitForResult(cluster);
    }

    /**
     * Send a message to remove a cache entry in a blocking manner.
     *
     * @param cluster The Aeron Cluster instance to use.
     * @param cacheId The ID of the cache we're removing an entry from.
     * @param key     The key of the entry we're removing.
     * @param c       The consumer that will handle the result.
     */
    public void removeCacheEntryBlocking(AeronCluster cluster, long cacheId, String key, Consumer<RemoveCacheEntryResult<Long, String>> c) {
        client.setRemoveCacheEntryConsumer(c);
        removeCacheEntryBlocking(cluster, cacheId, key);
        client.setRemoveCacheEntryConsumer(null);
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
