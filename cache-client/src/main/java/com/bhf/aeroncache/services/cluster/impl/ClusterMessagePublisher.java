package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.services.cluster.ClusterRequestPublisher;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

/**
 * Use this to publish the requests pre-encoded into the format expected by Aeron Cache.
 * <p>
 * A basic message publisher with no duty cycle. Simply
 * polls on the egress based on the method being called by the user.
 */
@Setter
@Log4j2
public class ClusterMessagePublisher implements ClusterRequestPublisher {

    private final MutableDirectBuffer msgBuffer = new ExpandableDirectByteBuffer();

    @Getter
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy();

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    private final AddCacheEntryEncoder addCacheEntryEncoder = new AddCacheEntryEncoder();
    private final GetCacheEntryEncoder getCacheEntryEncoder = new GetCacheEntryEncoder();
    private final ClearCacheEncoder clearCacheEncoder = new ClearCacheEncoder();
    private final DeleteCacheEncoder deleteCacheEncoder = new DeleteCacheEncoder();
    private final RemoveCacheEntryEncoder removeCacheEntryEncoder = new RemoveCacheEntryEncoder();
    private final GetAllCacheEntriesEncoder getAllCacheEntriesEncoder = new GetAllCacheEntriesEncoder();
    private final GetCacheStatsEncoder getCacheStatsEncoder = new GetCacheStatsEncoder();
    private final CacheSubscriptionRequestEncoder cacheSubscriptionRequestEncoder = new CacheSubscriptionRequestEncoder();
    private final CacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder = new CacheUnsubscribeRequestEncoder();

    @Override
    public void sendCreateCacheBlocking(AeronCache cluster, String requestId, long cacheId) {
        sendCreateCache(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCreateCache(AeronCache cluster, String requestId, long cacheId) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        publishCreateCache(cluster, createCacheEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent create cache request on cache {} with request Id {}", cacheId, requestId);
    }


    /**
     * Publish the request to create a new cache on the cluster.
     *
     * @param cluster            The cluster to publish too.
     * @param createCacheEncoder The encoded create cache message.
     * @param headerEncoder      The header encoder.
     * @param msgBuffer          The buffer to use.
     * @param msgBufferOffset    The offset from which to publish.
     */
    public void publishCreateCache(AeronCache cluster, CreateCacheEncoder createCacheEncoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, createCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void addCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key, String value) {
        addCacheEntry(cluster, requestId, cacheId, key, value);
        waitForResult(cluster);
    }

    @Override
    public void addCacheEntry(AeronCache cluster, String requestId, long cacheId, String key, String value) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId)
                .key(key)
                .entryValue(value);
        publishAddCachEntry(cluster, addCacheEntryEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent add cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    /**
     * Publish the request to add a cache entry to the cluster.
     *
     * @param cluster         The cluster to publish too.
     * @param addCacheEntry   The add cache entry message encoded.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishAddCachEntry(AeronCache cluster, AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, addCacheEntry.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key) {
        getCacheEntry(cluster, requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntry(AeronCache cluster, String requestId, long cacheId, String key) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        publishGetCacheEntry(cluster, getCacheEntryEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent get cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    /**
     * Publish a request to get an entry from the cluster.
     *
     * @param cluster         The cluster to get from.
     * @param getCacheEntry   The encoded get entry request.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishGetCacheEntry(AeronCache cluster, GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, getCacheEntryEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void clearCacheBlocking(AeronCache cluster, String requestId, long cacheId) {
        clearCache(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void clearCache(AeronCache cluster, String requestId, long cacheId) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishClearCache(cluster, clearCacheEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent clear cache request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish a request to clear a cache.
     *
     * @param cluster         The cluster on which the cache resides.
     * @param clearCache      The encoded request to clear a cache.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishClearCache(AeronCache cluster, ClearCacheEncoder clearCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, clearCache.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void deleteCacheBlocking(AeronCache cluster, String requestId, long cacheId) {
        deleteCache(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void deleteCache(AeronCache cluster, String requestId, long cacheId) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishDeleteCache(cluster, deleteCacheEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent delete cache request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish a request to delete an entire cache.
     *
     * @param cluster         The cluster on which the cache resides.
     * @param deleteCache     The encoded request to delete a cache.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishDeleteCache(AeronCache cluster, DeleteCacheEncoder deleteCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, deleteCache.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void removeCacheEntryBlocking(AeronCache cluster, String requestId, long cacheId, String key) {
        removeCacheEntry(cluster, requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void removeCacheEntry(AeronCache cluster, String requestId, long cacheId, String key) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        publishRemoveCacheEntry(cluster, removeCacheEntryEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent remove cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    /**
     * Publish a request to remove a cache entry.
     *
     * @param cluster          The cluster on which the cache resides.
     * @param removeCacheEntry The encoded request to remove a cache entry.
     * @param header           The message header.
     * @param msgBuffer        The buffer to use.
     * @param msgBufferOffset  The offset within the buffer to start.
     */
    void publishRemoveCacheEntry(AeronCache cluster, RemoveCacheEntryEncoder removeCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, removeCacheEntry.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getCacheEntriesBlocking(AeronCache cluster, String requestId, long cacheId) {
        getCacheEntries(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntries(AeronCache cluster, String requestId, long cacheId) {
        getAllCacheEntriesEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishGetAllCacheEntries(cluster, getAllCacheEntriesEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent get cache content request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish a request to get all cache entries.
     *
     * @param cluster                   The cluster on which the cache resides.
     * @param getAllCacheEntriesEncoder The encoded request.
     * @param header                    The message header.
     * @param msgBuffer                 The buffer to use.
     * @param msgBufferOffset           The offset within the buffer to start.
     */
    private void publishGetAllCacheEntries(AeronCache cluster, GetAllCacheEntriesEncoder getAllCacheEntriesEncoder, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, getAllCacheEntriesEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getAllCacheStatsBlocking(AeronCache cluster, String requestId) {
        getAllCacheStats(cluster, requestId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheSubscribeBlocking(AeronCache cluster, String requestId, long cacheId) {
        sendCacheSubscribe(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheSubscribe(AeronCache cluster, String requestId, long cacheId) {
        cacheSubscriptionRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishCacheSubscribe(cluster, cacheSubscriptionRequestEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent cache subscription request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish the request to subscribe to a cache on the cluster.
     *
     * @param cluster                  The cluster to publish too.
     * @param cacheSubscriptionRequest The encoded cache subscription request.
     * @param header                   The header encoder.
     * @param msgBuffer                The buffer to use.
     * @param msgBufferOffset          The offset from which to publish.
     */
    private void publishCacheSubscribe(AeronCache cluster, CacheSubscriptionRequestEncoder cacheSubscriptionRequest, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, cacheSubscriptionRequest.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void sendCacheUnsubscribeBlocking(AeronCache cluster, String requestId, long cacheId) {
        sendCacheUnsubscribe(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheUnsubscribe(AeronCache cluster, String requestId, long cacheId) {
        cacheUnsubscribeRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishCacheUnsubscribe(cluster, cacheUnsubscribeRequestEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent cache unsubscribe request on cache {} with request Id {}", cacheId, requestId);
    }

    private void publishCacheUnsubscribe(AeronCache cluster, CacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, cacheUnsubscribeRequestEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getAllCacheStats(AeronCache cluster, String requestId) {
        getCacheStatsEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .requestId(requestId);
        publishGetAllCacheStats(cluster, getCacheStatsEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent request to get all cache with request Id {}", requestId);
    }

    /**
     * Publish a request to get all cache stats.
     *
     * @param cluster         The cluster on which the cache resides.
     * @param getCacheStats   The encoded request to get cache stats.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishGetAllCacheStats(AeronCache cluster, GetCacheStatsEncoder getCacheStats, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, getCacheStats.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    /**
     * Wait for results back from the cluster.
     *
     * @param cluster The Aeron Cluster.
     */
    private void waitForResult(AeronCache cluster) {
        pollEgressUntilMessage(this.getIdleStrategy(), cluster);
    }

    /**
     * Poll the egress of the cluster.
     *
     * @param cluster The cluster to poll.
     * @return Number of fragments processed.
     */
    int pollEgress(AeronCache cluster) {
        return null == cluster ? 0 : cluster.pollEgress();
    }

    /**
     * Keep polling the egress till we get a message.
     *
     * @param idleStrategy The idle strategy to use.
     * @param cluster      The cluster to poll.
     */
    void pollEgressUntilMessage(IdleStrategy idleStrategy, AeronCache cluster) {
        idleStrategy.reset();
        while (pollEgress(cluster) <= 0) {
            idleStrategy.idle();
        }
    }
}
