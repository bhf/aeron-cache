package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.codecs.CacheRequestEncoder;
import com.bhf.aeroncache.handlers.NoOpPublicationFailureHandler;
import com.bhf.aeroncache.handlers.PublicationFailureHandler;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cluster.BlockingClusterRequestPublisher;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
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
    private final IdleStrategy idleStrategy;
    private final PublicationFailureHandler publicationFailureHandler = new NoOpPublicationFailureHandler();
    private final CacheRequestEncoder cacheRequestEncoder;

    @Override
    public void sendCreateCacheBlocking(String requestId, String cacheId) {
        sendCreateCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCreateCache(String requestId, String cacheId) {
        var length = cacheRequestEncoder.encodeCreateCacheRequest(requestId, cacheId, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent create cache request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void addCacheEntryBlocking(String requestId, String cacheId, String key, String value) {
        addCacheEntry(requestId, cacheId, key, value);
        waitForResult(cluster);
    }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value) {
        var length = cacheRequestEncoder.encodeAddCacheEntry(requestId, cacheId, key, value, msgBuffer);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent add cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    @Override
    public void getCacheEntryBlocking(String requestId, String cacheId, String key) {
        getCacheEntry(requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntry(String requestId, String cacheId, String key) {
        var length = cacheRequestEncoder.encodeGetCacheEntry(msgBuffer, requestId, cacheId, key);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent get cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    @Override
    public void clearCacheBlocking(String requestId, String cacheId) {
        clearCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void clearCache(String requestId, String cacheId) {
        var length = cacheRequestEncoder.encodeClearCache(msgBuffer, requestId, cacheId);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent clear cache request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void deleteCacheBlocking(String requestId, String cacheId) {
        deleteCache(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void deleteCache(String requestId, String cacheId) {
        var length = cacheRequestEncoder.encodeDeleteCache(msgBuffer, requestId, cacheId);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent delete cache request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void removeCacheEntryBlocking(String requestId, String cacheId, String key) {
        removeCacheEntry(requestId, cacheId, key);
        waitForResult(cluster);
    }

    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key) {
        var length = cacheRequestEncoder.encodeRemoveCacheEntry(msgBuffer, requestId, cacheId, key);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent remove cache entry request on cache {}, key {}, with request Id {}", cacheId, key, requestId);
    }

    @Override
    public void getCacheEntriesBlocking(String requestId, String cacheId) {
        getCacheEntries(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId) {
        var length = cacheRequestEncoder.encodeGetCacheEntries(msgBuffer, requestId, cacheId);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent get cache content request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void sendCacheSubscribeBlocking(String requestId, String cacheId) {
        sendCacheSubscribe(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheSubscribe(String requestId, String cacheId) {
        var length = cacheRequestEncoder.encodeCacheSubscribe(msgBuffer, requestId, cacheId);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent cache subscription request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void sendCacheUnsubscribeBlocking(String requestId, String cacheId) {
        sendCacheUnsubscribe(requestId, cacheId);
        waitForResult(cluster);
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId) {
        var length = cacheRequestEncoder.encodeCacheUnsubscribe(msgBuffer, requestId, cacheId);
        publishToCache(msgBuffer, 0, length);
        log.info("Sent cache unsubscribe request on cache {} with request Id {}", cacheId, requestId);
    }

    @Override
    public void getAllCacheStatsBlocking(String requestId) {
        getAllCacheStats(requestId);
        waitForResult(cluster);
    }

    @Override
    public void getAllCacheStats(String requestId) {
        var length = cacheRequestEncoder.encodeGetAllCacheStats(msgBuffer, requestId);
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
