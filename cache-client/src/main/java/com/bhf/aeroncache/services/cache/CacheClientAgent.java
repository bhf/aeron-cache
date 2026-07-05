package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.bhf.aeroncache.services.AbstractClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import java.util.ArrayList;
import java.util.List;

import static com.bhf.aeroncache.models.CacheRequestMessageTypes.*;

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


    public CacheClientAgent(AeronCache cluster, RingBuffer rb, IdleStrategy idleStrategy, ClusterMessagePublisher publisher, String roleName) {
        super(cluster, rb, idleStrategy, publisher, roleName);
    }

    @Override
    public void processInboundMessages(RingBuffer rb) {
        rb.read((msgTypeId, buffer, index, length) -> {
            log.debug("Got msg ID " + msgTypeId + " at index " + index + ", length=" + length);

            switch (msgTypeId) {
                case CREATE_CACHE_MSG_ID -> handleCreateCache(buffer, index);
                case ADD_CACHE_ENTRY_MSG_ID -> handleAddCacheEntry(buffer, index);
                case GET_CACHE_ENTRY_MSG_ID -> handleGetCacheEntry(buffer, index);
                case CLEAR_CACHE_MSG_ID -> handleClearCache(buffer, index);
                case DELETE_CACHE_MSG_ID -> handleDeleteCache(buffer, index);
                case GET_CACHE_ENTRIES_MSG_ID -> handleGetCacheEntries(buffer, index);
                case SUBSCRIBE_TO_CACHE_MSG_ID -> handleSubscribeToCache(buffer, index);
                case UNSUBSCRIBE_TO_CACHE_MSG_ID -> handleUnsubscribeToCache(buffer, index);
                case GET_CACHE_STATS_MSG_ID -> handleGetCacheStats(buffer, index);
                case REMOVE_CACHE_ENTRY_MSG_ID -> handleRemoveCacheEntry(buffer, index);
                case BULK_OPS_MSG_ID -> handleBulkOps(buffer, index);
                default -> log.warn("Got unknown msgType: {} processing inbound client cache requests", msgTypeId);
            }
        });
    }

    private void handleBulkOps(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var opCount = buffer.getInt(cumulativeReadPosition);
        cumulativeReadPosition += 4;
        List<CacheOperationRequest> operations = new ArrayList<>();

        for (int i = 0; i < opCount; i++) {
            var opRequestId = buffer.getStringUtf8(cumulativeReadPosition);
            cumulativeReadPosition += (buffer.getInt(cumulativeReadPosition) + 4);

            var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
            cumulativeReadPosition += (buffer.getInt(cumulativeReadPosition) + 4);

            var key = buffer.getStringUtf8(cumulativeReadPosition);
            cumulativeReadPosition += (buffer.getInt(cumulativeReadPosition) + 4);

            var value = buffer.getStringUtf8(cumulativeReadPosition);
            cumulativeReadPosition += (buffer.getInt(cumulativeReadPosition) + 4);

            var ttl = buffer.getLong(cumulativeReadPosition);
            cumulativeReadPosition += 8;

            var ordinal = buffer.getInt(cumulativeReadPosition);
            var opType = BulkOperationType.values()[ordinal];
            cumulativeReadPosition += 4;

            CacheOperationRequest r = new CacheOperationRequest(opType, ttl, 0, opRequestId, cacheId, key, value);
            operations.add(r);
        }

        BulkCacheOpsRequest request = new BulkCacheOpsRequest(requestId, operations);
        getPublisher().sendBulkOperationsRequest(requestId, request);
    }

    private void handleRemoveCacheEntry(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var key = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("REMOVE CACHE ENTRY Request has ID " + requestId + ", cache ID " + cacheId + ", remove key=" + key);
        getPublisher().removeCacheEntry(requestId, cacheId, key);
    }

    private void handleGetCacheStats(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        log.debug("GET CACHE STATS Request has ID " + requestId);
        getPublisher().getAllCacheStats(requestId);
    }

    private void handleUnsubscribeToCache(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("UNSUBSCRIBE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
        getPublisher().sendCacheUnsubscribe(requestId, cacheId);
    }

    private void handleSubscribeToCache(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);

        var cacheIdCount = buffer.getInt(cumulativeReadPosition);
        cumulativeReadPosition += 4;

        List<String> cacheIds = new ArrayList<>();
        for (int i = 0; i < cacheIdCount; i++) {
            var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
            cacheIds.add(cacheId);
            cumulativeReadPosition += (buffer.getInt(cumulativeReadPosition) + 4);
        }

        boolean sendSnapshot = buffer.getByte(cumulativeReadPosition) == (byte) 1;
        log.debug("SUBSCRIBE CACHE Request has ID " + requestId + ", on cache IDs " + cacheIds);
        getPublisher().sendCacheSubscribe(requestId, cacheIds, sendSnapshot);
    }

    private void handleGetCacheEntries(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("GET CACHE ENTRIES Request has ID " + requestId + ", on cache ID " + cacheId);
        getPublisher().getCacheEntries(requestId, cacheId);
    }

    private void handleDeleteCache(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("DELETE CACHE Request has ID " + requestId + ", to delete cache ID " + cacheId);
        getPublisher().deleteCache(requestId, cacheId);
    }

    private void handleClearCache(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cacheId = buffer.getStringUtf8(index + buffer.getInt(index) + 4);
        log.debug("CLEAR CACHE Request has ID " + requestId + ", to clear on cache ID " + cacheId);
        getPublisher().clearCache(requestId, cacheId);
    }

    private void handleGetCacheEntry(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var key = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("GET CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", to get key=" + key);
        getPublisher().getCacheEntry(requestId, cacheId, key);
    }

    private void handleAddCacheEntry(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var key = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var value = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var ttl = buffer.getLong(cumulativeReadPosition);
        log.debug("ADD CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", key=" + key + ", value=" + value+", ttl="+ttl);
        getPublisher().addCacheEntry(requestId, cacheId, key, value, ttl);
    }

    private void handleCreateCache(MutableDirectBuffer buffer, int index) {
        var requestId = buffer.getStringUtf8(index);
        var cacheId = buffer.getStringUtf8(index + buffer.getInt(index) + 4);
        log.debug("CREATE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
        getPublisher().sendCreateCache(requestId, cacheId);
    }
}
