package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.CountersCacheRequestEncoder;
import com.bhf.aeroncache.handlers.NoOpPublicationFailureHandler;
import com.bhf.aeroncache.handlers.PublicationFailureHandler;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cluster.BlockingClusterRequestPublisher;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.List;

/**
 * Encode cache requests into SBE and send them to the cluster.
 */
@Setter
@Log4j2
@RequiredArgsConstructor
public class ClusterMessagePublisher<BI, BK, BV> implements CacheRequestPublisher<BI, BK, BV>, BlockingClusterRequestPublisher<BI, BK, BV> {

    private final MutableDirectBuffer msgBuffer = new ExpandableDirectByteBuffer();
    private final AeronCache cluster;
    private final IdleStrategy idleStrategy;
    private final PublicationFailureHandler publicationFailureHandler = new NoOpPublicationFailureHandler();
    private final CacheRequestEncoder<BI, BK, BV> cacheRequestEncoder;

    @Override
    public void sendCreateCacheBlocking(String requestId, BI cacheId) {
        sendCreateCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCreateCache(String requestId, BI cacheId) {
        var length = cacheRequestEncoder.encodeCreateCacheRequest(requestId, cacheId, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent create cache request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void addCacheEntryBlocking(String requestId, BI cacheId, BK key, BV value, long ttl) {
        addCacheEntry(requestId, cacheId, key, value, ttl);
        waitForResult(cluster);
    }

    @Override
    public void addCacheEntry(String requestId, BI cacheId, BK key, BV value, long ttl) {
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, ttl, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent add cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    @Override
    public void getCacheEntryBlocking(String requestId, BI cacheId, BK key) {
        getCacheEntry(requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntry(String requestId, BI cacheId, BK key) {
        var length = cacheRequestEncoder.encodeGetCacheEntry(requestId, cacheId, key, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent get cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    @Override
    public void clearCacheBlocking(String requestId, BI cacheId) {
        clearCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void clearCache(String requestId, BI cacheId) {
        var length = cacheRequestEncoder.encodeClearCache(requestId, cacheId, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent clear cache request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void deleteCacheBlocking(String requestId, BI cacheId) {
        deleteCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void deleteCache(String requestId, BI cacheId) {
        var length = cacheRequestEncoder.encodeDeleteCache(requestId, cacheId, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent delete cache request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void removeCacheEntryBlocking(String requestId, BI cacheId, BK key) {
        removeCacheEntry(requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void removeCacheEntry(String requestId, BI cacheId, BK key) {
        var length = cacheRequestEncoder.encodeRemoveCacheEntry(requestId, cacheId, key, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent remove cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    @Override
    public void incrementCounter(String requestId, BI cacheId, BK key, long amount, long ttl) {
        var length = ((CountersCacheRequestEncoder<BI, BK, BV>) cacheRequestEncoder).encodeIncrementCounterRequest(requestId, cacheId, key, amount, ttl, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent increment counter request on cache {}, key {}, amount {}, with request Id {}", cacheId, key, amount, requestId);
    }

    @Override
    public void decrementCounter(String requestId, BI cacheId, BK key, long amount, long ttl) {
        var length = ((CountersCacheRequestEncoder<BI, BK, BV>) cacheRequestEncoder).encodeDecrementCounterRequest(requestId, cacheId, key, amount, ttl, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent decrement counter request on cache {}, key {}, amount {}, with request Id {}", cacheId, key, amount, requestId);
    }

    @Override
    public void setCounter(String requestId, BI cacheId, BK key, long value, long ttl) {
        var length = ((CountersCacheRequestEncoder<BI, BK, BV>) cacheRequestEncoder).encodeSetCounterRequest(requestId, cacheId, key, value, ttl, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent set counter request on cache {}, key {}, value {}, with request Id {}", cacheId, key, value, requestId);
    }

    @Override
    public void getCacheEntriesBlocking(String requestId, BI cacheId) {
        getCacheEntries(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntries(String requestId, BI cacheId) {
        var length = cacheRequestEncoder.encodeGetCacheEntries(requestId, cacheId, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent get cache content request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void sendCacheSubscribeBlocking(String requestId, List<BI> cacheId, boolean sendSnapshot) {
        sendCacheSubscribe(requestId, cacheId, sendSnapshot);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheSubscribe(String requestId, List<BI> cacheId, boolean sendSnapshot) {
        var length = cacheRequestEncoder.encodeCacheSubscribe(requestId, cacheId, sendSnapshot, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent cache subscription request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void sendCacheUnsubscribeBlocking(String requestId, BI cacheId) {
        sendCacheUnsubscribe(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, BI cacheId) {
        var length = cacheRequestEncoder.encodeCacheUnsubscribe(requestId, cacheId, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent cache unsubscribe request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void sendBulkOperationsBlocking(String requestId, BulkCacheOpsRequest request) {
        sendBulkOperationsRequest(requestId, request);
        waitForResult(cluster);
    }

    @Override
    public void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request) {
        var length = cacheRequestEncoder.encodeBulkOperations(requestId, request, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent bulk operation request with request Id {}",      requestId);
    }

    @Override
    public void getAllCacheStatsBlocking(String requestId) {
        getAllCacheStats(requestId);
        waitForResult(cluster);
    }

    @Override
    public void getAllCacheStats(String requestId) {
        var length = cacheRequestEncoder.encodeGetAllCacheStats(requestId, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent request to get all cache with request Id {}", requestId);
    }

    public void publishToCache(MutableDirectBuffer msgBuffer, int offset, int length) {
        idleStrategy.reset();
        long offered = 0;
        while ((offered = cluster.offer(msgBuffer, offset, length)) < 0) {
            publicationFailureHandler.handleOfferFailure(offered);
            idleStrategy.idle(cluster.pollEgress());
        }
    }

    /**
     * Wait for results back from the cluster.
     *
     * @param cluster The Aeron Cluster.
     */
    private void waitForResult(AeronCache cluster) {
        pollEgressUntilMessage(idleStrategy, cluster);
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
