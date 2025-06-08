package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import io.aeron.cluster.client.AeronCluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.bhf.aeroncache.services.cache.impl.CacheRequestMessageTypes.*;

/**
 * An {@link Agent} implementation of an AeronCache Client that is run
 * via an {@link org.agrona.concurrent.AgentRunner}.
 * <p>
 * Processes cache requests encoded in an internal format.
 * <p>
 * A good option when you're worried about head of line blocking on
 * the client publishing side (you do any SBE encoding and logic on this Agent thread).
 */
@Log4j2
@RequiredArgsConstructor
public class CacheClientAgent implements Agent {

    final AeronCluster cluster;
    final ManyToOneRingBuffer rb;
    final IdleStrategy idleStrategy;
    final ClusterMessagePublisher publisher;
    final AtomicBoolean isEnabled = new AtomicBoolean(true);
    private final int KEEPALIVE_INTERVAL = 200;
    long lastKeepAlive = 0;

    @Override
    public void onStart() {
        log.info("Starting cluster client agent");
        Agent.super.onStart();
    }

    /**
     * The core duty cycle of the Agent. Checks the request queue
     * for requests to be encoded for the cache, handles heartbeats
     * and also polling the egress for messages from the cluster.
     *
     * @return
     * @throws Exception
     */
    @Override
    public int doWork() throws Exception {
        while (isEnabled.get()) {
            handleKeepAlive(cluster);
            processInboundMessages(rb);
            cluster.pollEgress();
            idleStrategy.idle();
        }

        return 0;
    }

    private void processInboundMessages(ManyToOneRingBuffer rb) {
        rb.read((msgTypeId, buffer, index, length) -> {
            log.info("Got msg ID " + msgTypeId + " at index " + index + ", length=" + length);

            switch (msgTypeId) {
                case CREATE_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cacheId = buffer.getLong(index + requestId.length() + 4);
                    log.info("CREATE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
                    publisher.sendCreateCache(cluster, requestId, cacheId);
                }
                case ADD_CACHE_ENTRY_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    cumulativeReadPosition += 8;
                    var key = buffer.getStringUtf8(cumulativeReadPosition);
                    cumulativeReadPosition += key.length() + 4;
                    var value = buffer.getStringUtf8(cumulativeReadPosition);
                    log.info("ADD CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", key=" + key + ", value=" + value);
                    publisher.addCacheEntry(cluster, requestId, cacheId, key, value);
                }
                case GET_CACHE_ENTRY_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    cumulativeReadPosition += 8;
                    var key = buffer.getStringUtf8(cumulativeReadPosition);
                    log.info("GET CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", to get key=" + key);
                    publisher.getCacheEntry(cluster, requestId, cacheId, key);
                }
                case CLEAR_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cacheId = buffer.getLong(index + requestId.length() + 4);
                    log.info("CLEAR CACHE Request has ID " + requestId + ", to clear on cache ID " + cacheId);
                    publisher.clearCache(cluster, requestId, cacheId);
                }
                case DELETE_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    log.info("DELETE CACHE Request has ID " + requestId + ", to delete cache ID " + cacheId);
                    publisher.deleteCache(cluster, requestId, cacheId);
                }
                case GET_CACHE_ENTRIES_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    log.info("GET CACHE ENTRIES Request has ID " + requestId + ", on cache ID " + cacheId);
                    publisher.getCacheEntries(cluster, requestId, cacheId);
                }
                case SUBSCRIBE_TO_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    log.info("SUBSCRIBE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
                    publisher.sendCacheSubscribe(cluster, requestId, cacheId);
                }
                case UNSUBSCRIBE_TO_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    log.info("UNSUBSCRIBE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
                    publisher.sendCacheUnsubscribe(cluster, requestId, cacheId);
                }
                case GET_CACHE_STATS_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    log.info("GET CACHE STATS Request has ID " + requestId);
                    publisher.getAllCacheStats(cluster, requestId);
                }
                case REMOVE_CACHE_ENTRY_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    cumulativeReadPosition += 8;
                    var key = buffer.getStringUtf8(cumulativeReadPosition);
                    log.info("REMOVE CACHE ENTRY Request has ID " + requestId + ", cache ID " + cacheId + ", remove key=" + key);
                    publisher.removeCacheEntry(cluster, requestId, cacheId, key);
                }
                default -> log.warn("Got unknown msgType: {} processing inbound client cache requests", msgTypeId);
            }

        });
    }

    private void handleKeepAlive(AeronCluster cluster) {
        long now = System.currentTimeMillis();

        if (now > lastKeepAlive + KEEPALIVE_INTERVAL) {
            cluster.sendKeepAlive();
            lastKeepAlive = now;
        }
    }

    @Override
    public void onClose() {
        log.info("Closing cluster client agent");
        Agent.super.onClose();
    }

    @Override
    public String roleName() {
        return "AeronCache-CacheClient-Agent";
    }
}
