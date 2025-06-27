package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.services.AbstractClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import static com.bhf.aeroncache.model.CacheRequestMessageTypes.*;

/**
 * An {@link Agent} implementation of an AeronCache Client that is run
 * via an {@link org.agrona.concurrent.AgentRunner}.
 * <p>
 * Processes cache requests encoded in an internal format.
 * <p>
 * A good option when you're worried about head of line blocking on
 * the client publishing side (you do any SBE encoding and logic on this Agent thread).
 * <p>
 * Allows for having multiple client side publishing threads publishing to a
 * {@link ManyToOneRingBuffer} instance.
 */
@Log4j2
public class CacheClientAgent extends AbstractClientAgent {


    public CacheClientAgent(AeronCache cluster, ManyToOneRingBuffer rb, IdleStrategy idleStrategy, ClusterMessagePublisher publisher, String roleName) {
        super(cluster, rb, idleStrategy, publisher, roleName);
    }

    @Override
    public void processInboundMessages(ManyToOneRingBuffer rb) {
        rb.read((msgTypeId, buffer, index, length) -> {
            log.debug("Got msg ID " + msgTypeId + " at index " + index + ", length=" + length);

            switch (msgTypeId) {
                case CREATE_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cacheId = buffer.getLong(index + requestId.length() + 4);
                    log.debug("CREATE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
                    getPublisher().sendCreateCache(requestId, cacheId);
                }
                case ADD_CACHE_ENTRY_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    cumulativeReadPosition += 8;
                    var key = buffer.getStringUtf8(cumulativeReadPosition);
                    cumulativeReadPosition += key.length() + 4;
                    var value = buffer.getStringUtf8(cumulativeReadPosition);
                    log.debug("ADD CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", key=" + key + ", value=" + value);
                    getPublisher().addCacheEntry(requestId, cacheId, key, value);
                }
                case GET_CACHE_ENTRY_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    cumulativeReadPosition += 8;
                    var key = buffer.getStringUtf8(cumulativeReadPosition);
                    log.debug("GET CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", to get key=" + key);
                    getPublisher().getCacheEntry(requestId, cacheId, key);
                }
                case CLEAR_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cacheId = buffer.getLong(index + requestId.length() + 4);
                    log.debug("CLEAR CACHE Request has ID " + requestId + ", to clear on cache ID " + cacheId);
                    getPublisher().clearCache(requestId, cacheId);
                }
                case DELETE_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    log.debug("DELETE CACHE Request has ID " + requestId + ", to delete cache ID " + cacheId);
                    getPublisher().deleteCache(requestId, cacheId);
                }
                case GET_CACHE_ENTRIES_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    log.debug("GET CACHE ENTRIES Request has ID " + requestId + ", on cache ID " + cacheId);
                    getPublisher().getCacheEntries(requestId, cacheId);
                }
                case SUBSCRIBE_TO_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    log.debug("SUBSCRIBE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
                    getPublisher().sendCacheSubscribe(requestId, cacheId);
                }
                case UNSUBSCRIBE_TO_CACHE_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    log.debug("UNSUBSCRIBE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
                    getPublisher().sendCacheUnsubscribe(requestId, cacheId);
                }
                case GET_CACHE_STATS_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    log.debug("GET CACHE STATS Request has ID " + requestId);
                    getPublisher().getAllCacheStats(requestId);
                }
                case REMOVE_CACHE_ENTRY_MSG_ID -> {
                    var requestId = buffer.getStringUtf8(index);
                    var cumulativeReadPosition = index + (requestId.length() + 4);
                    var cacheId = buffer.getLong(cumulativeReadPosition);
                    cumulativeReadPosition += 8;
                    var key = buffer.getStringUtf8(cumulativeReadPosition);
                    log.debug("REMOVE CACHE ENTRY Request has ID " + requestId + ", cache ID " + cacheId + ", remove key=" + key);
                    getPublisher().removeCacheEntry(requestId, cacheId, key);
                }
                default -> log.warn("Got unknown msgType: {} processing inbound client cache requests", msgTypeId);
            }

        });
    }
}
