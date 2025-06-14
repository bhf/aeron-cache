package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cluster.BlockingClusterRequestPublisher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

/**
 * Encode cache requests into SBE and send them to the cluster.
 */
@Setter
@Log4j2
@RequiredArgsConstructor
public class ClusterMessagePublisher implements CacheRequestPublisher, BlockingClusterRequestPublisher {

    private final MutableDirectBuffer msgBuffer = new ExpandableDirectByteBuffer();
    private final AeronCache cluster;

    @Getter
    private final IdleStrategy idleStrategy = new BusySpinIdleStrategy();

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
    public void sendCreateCacheBlocking(String requestId, long cacheId) {
        sendCreateCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCreateCache(String requestId, long cacheId) {
        CacheRequestEncoder.encodeCreateCacheRequest(createCacheEncoder, headerEncoder, msgBuffer, requestId, cacheId);
        publishCreateCache(createCacheEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent create cache request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish the request to create a new cache on the cluster.
     *
     * @param createCacheEncoder The encoded create cache message.
     * @param headerEncoder      The header encoder.
     * @param msgBuffer          The buffer to use.
     * @param msgBufferOffset    The offset from which to publish.
     */
    public void publishCreateCache(CreateCacheEncoder createCacheEncoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, createCacheEncoder.encodedLength() + headerEncoder.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void addCacheEntryBlocking(String requestId, long cacheId, String key, String value) {
        addCacheEntry(requestId, cacheId, key, value);
        waitForResult(cluster);
    }

    @Override
    public void addCacheEntry(String requestId, long cacheId, String key, String value) {
        CacheRequestEncoder.encodeAddCacheEntry(addCacheEntryEncoder, headerEncoder, msgBuffer, requestId, cacheId, key, value);
        publishAddCachEntry(addCacheEntryEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent add cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    /**
     * Publish the request to add a cache entry to the cluster.
     *
     * @param addCacheEntry   The add cache entry message encoded.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishAddCachEntry(AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, addCacheEntry.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getCacheEntryBlocking(String requestId, long cacheId, String key) {
        getCacheEntry(requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntry(String requestId, long cacheId, String key) {
        CacheRequestEncoder.encodeGetCacheEntry(getCacheEntryEncoder, headerEncoder, msgBuffer, requestId, cacheId, key);
        publishGetCacheEntry(getCacheEntryEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent get cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    /**
     * Publish a request to get an entry from the cluster.
     *
     * @param getCacheEntry   The encoded get entry request.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishGetCacheEntry(GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, getCacheEntryEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void clearCacheBlocking(String requestId, long cacheId) {
        clearCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void clearCache(String requestId, long cacheId) {
        CacheRequestEncoder.encodeClearCache(clearCacheEncoder, headerEncoder, msgBuffer, requestId, cacheId);
        publishClearCache(clearCacheEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent clear cache request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish a request to clear a cache.
     *
     * @param clearCache      The encoded request to clear a cache.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishClearCache(ClearCacheEncoder clearCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, clearCache.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void deleteCacheBlocking(String requestId, long cacheId) {
        deleteCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void deleteCache(String requestId, long cacheId) {
        CacheRequestEncoder.encodeDeleteCache(deleteCacheEncoder, headerEncoder, msgBuffer, requestId, cacheId);
        publishDeleteCache(deleteCacheEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent delete cache request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish a request to delete an entire cache.
     *
     * @param deleteCache     The encoded request to delete a cache.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishDeleteCache(DeleteCacheEncoder deleteCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, deleteCache.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void removeCacheEntryBlocking(String requestId, long cacheId, String key) {
        removeCacheEntry(requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void removeCacheEntry(String requestId, long cacheId, String key) {
        CacheRequestEncoder.encodeRemoveCacheEntry(removeCacheEntryEncoder, headerEncoder, msgBuffer, requestId, cacheId, key);
        publishRemoveCacheEntry(removeCacheEntryEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent remove cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    /**
     * Publish a request to remove a cache entry.
     *
     * @param removeCacheEntry The encoded request to remove a cache entry.
     * @param header           The message header.
     * @param msgBuffer        The buffer to use.
     * @param msgBufferOffset  The offset within the buffer to start.
     */
    void publishRemoveCacheEntry(RemoveCacheEntryEncoder removeCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, removeCacheEntry.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getCacheEntriesBlocking(String requestId, long cacheId) {
        getCacheEntries(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntries(String requestId, long cacheId) {
        CacheRequestEncoder.encodeGetCacheEntries(getAllCacheEntriesEncoder, headerEncoder, msgBuffer, requestId, cacheId);
        publishGetAllCacheEntries(getAllCacheEntriesEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent get cache content request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish a request to get all cache entries.
     *
     * @param getAllCacheEntriesEncoder The encoded request.
     * @param header                    The message header.
     * @param msgBuffer                 The buffer to use.
     * @param msgBufferOffset           The offset within the buffer to start.
     */
    void publishGetAllCacheEntries(GetAllCacheEntriesEncoder getAllCacheEntriesEncoder, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, getAllCacheEntriesEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getAllCacheStatsBlocking(String requestId) {
        getAllCacheStats(requestId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheSubscribeBlocking(String requestId, long cacheId) {
        sendCacheSubscribe(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheSubscribe(String requestId, long cacheId) {
        CacheRequestEncoder.encodeCacheSubscribe(cacheSubscriptionRequestEncoder, headerEncoder, msgBuffer, requestId, cacheId);
        publishCacheSubscribe(cacheSubscriptionRequestEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent cache subscription request on cache {} with request Id {}", cacheId, requestId);
    }

    /**
     * Publish the request to subscribe to a cache on the cluster.
     *
     * @param cacheSubscriptionRequest The encoded cache subscription request.
     * @param header                   The header encoder.
     * @param msgBuffer                The buffer to use.
     * @param msgBufferOffset          The offset from which to publish.
     */
    void publishCacheSubscribe(CacheSubscriptionRequestEncoder cacheSubscriptionRequest, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, cacheSubscriptionRequest.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void sendCacheUnsubscribeBlocking(String requestId, long cacheId) {
        sendCacheUnsubscribe(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, long cacheId) {
        CacheRequestEncoder.encodeCacheUnsubscribe(cacheUnsubscribeRequestEncoder, headerEncoder, msgBuffer, requestId, cacheId);
        publishCacheUnsubscribe(cacheUnsubscribeRequestEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent cache unsubscribe request on cache {} with request Id {}", cacheId, requestId);
    }

    void publishCacheUnsubscribe(CacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        idleStrategy.reset();
        while (cluster.offer(msgBuffer, msgBufferOffset, cacheUnsubscribeRequestEncoder.encodedLength() + header.encodedLength()) < 0) {
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    @Override
    public void getAllCacheStats(String requestId) {
        CacheRequestEncoder.encodeGetAllCacheStats(getCacheStatsEncoder, headerEncoder, msgBuffer, requestId);
        publishGetAllCacheStats(getCacheStatsEncoder, headerEncoder, msgBuffer, 0);
        log.info("Sent request to get all cache with request Id {}", requestId);
    }

    /**
     * Publish a request to get all cache stats.
     *
     * @param getCacheStats   The encoded request to get cache stats.
     * @param header          The message header.
     * @param msgBuffer       The buffer to use.
     * @param msgBufferOffset The offset within the buffer to start.
     */
    void publishGetAllCacheStats(GetCacheStatsEncoder getCacheStats, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
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
