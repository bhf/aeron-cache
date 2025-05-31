package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.services.cluster.ClusterRequestPublisher;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.logbuffer.BufferClaim;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableDirectByteBuffer;
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

    public static final int BASE_TRY_CLAIM_SIZE = 512;
    private final MutableDirectBuffer msgBuffer = new ExpandableDirectByteBuffer();
    private boolean useTryClaim = true;

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
    public void sendCreateCacheBlocking(AeronCluster cluster, String requestId, long cacheId) {
        sendCreateCache(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCreateCache(AeronCluster cluster, String requestId, long cacheId) {
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        createCacheEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        publishCreateCache(cluster, createCacheEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    public void publishCreateCache(AeronCluster cluster, CreateCacheEncoder createCacheEncoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, createCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
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
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId)
                .key(key)
                .entryValue(value);
        publishAddCachEntry(cluster, addCacheEntryEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    void publishAddCachEntry(AeronCluster cluster, AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, addCacheEntry.encodedLength() + header.encodedLength()) < 0) {
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
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        publishGetCacheEntry(cluster, getCacheEntryEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    void publishGetCacheEntry(AeronCluster cluster, GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, getCacheEntryEncoder.encodedLength() + header.encodedLength()) < 0) {
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
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishClearCache(cluster, clearCacheEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    void publishClearCache(AeronCluster cluster, ClearCacheEncoder clearCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, clearCache.encodedLength() + header.encodedLength()) < 0) {
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
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishDeleteCache(cluster, deleteCacheEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    void publishDeleteCache(AeronCluster cluster, DeleteCacheEncoder deleteCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, deleteCache.encodedLength() + header.encodedLength()) < 0) {
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
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        publishRemoveCacheEntry(cluster, removeCacheEntryEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    void publishRemoveCacheEntry(AeronCluster cluster, RemoveCacheEntryEncoder removeCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, removeCacheEntry.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getCacheEntriesBlocking(AeronCluster cluster, String requestId, long cacheId) {
        getCacheEntries(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntries(AeronCluster cluster, String requestId, long cacheId) {
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        getAllCacheEntriesEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishGetAllCacheEntries(cluster, getAllCacheEntriesEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    private void publishGetAllCacheEntries(AeronCluster cluster, GetAllCacheEntriesEncoder getAllCacheEntriesEncoder, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, getAllCacheEntriesEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getAllCacheStatsBlocking(AeronCluster cluster, String requestId) {
        getAllCacheStats(cluster, requestId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheSubscribeBlocking(AeronCluster cluster, String requestId, long cacheId) {
        sendCacheSubscribe(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheSubscribe(AeronCluster cluster, String requestId, long cacheId) {
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        cacheSubscriptionRequestEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishCacheSubscribe(cluster, cacheSubscriptionRequestEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    private void publishCacheSubscribe(AeronCluster cluster, CacheSubscriptionRequestEncoder cacheSubscriptionRequest, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, cacheSubscriptionRequest.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void sendCacheUnsubscribeBlocking(AeronCluster cluster, String requestId, long cacheId) {
        sendCacheUnsubscribe(cluster, requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheUnsubscribe(AeronCluster cluster, String requestId, long cacheId) {
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        cacheUnsubscribeRequestEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        publishCacheUnsubscribe(cluster, cacheUnsubscribeRequestEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
        log.info("Sent cache unsubscribe request on cache {} with request Id {}", cacheId, requestId);
    }

    private void publishCacheUnsubscribe(AeronCluster cluster, CacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, cacheUnsubscribeRequestEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getAllCacheStats(AeronCluster cluster, String requestId) {
        var msgBuffer = this.msgBuffer;
        var msgBufferOffset = 0;
        BufferClaim bufferClaim = null;

        if (useTryClaim) {
            bufferClaim = new BufferClaim();
            cluster.tryClaim(BASE_TRY_CLAIM_SIZE, bufferClaim);
            msgBuffer = bufferClaim.buffer();
            msgBufferOffset = bufferClaim.offset();
        }

        getCacheStatsEncoder.wrapAndApplyHeader(msgBuffer, msgBufferOffset, headerEncoder)
                .requestId(requestId);
        publishGetAllCacheStats(cluster, getCacheStatsEncoder, headerEncoder, msgBuffer, msgBufferOffset);

        if (bufferClaim != null) {
            bufferClaim.commit();
        }
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
    void publishGetAllCacheStats(AeronCluster cluster, GetCacheStatsEncoder getCacheStats, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
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
