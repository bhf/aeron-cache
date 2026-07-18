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


    public CacheClientAgent(AeronCache cluster, RingBuffer rb, IdleStrategy idleStrategy,
                            ClusterMessagePublisher<String, String, String> cacheOpsPublisher,
                            ClusterMessagePublisher<String, String, Long> counterOpsPublisher,
                            String roleName) {
        super(cluster, rb, idleStrategy, cacheOpsPublisher, counterOpsPublisher, roleName);
    }

    @Override
    public void processInboundMessages(RingBuffer rb) {
        rb.read((msgTypeId, buffer, index, length) -> {
            log.debug("Got msg ID " + msgTypeId + " at index " + index + ", length=" + length);

            final var cacheOpsPublisher = getCachePublisher();
            final var counterOpsPublisher = getCounterOpsPublisher();

            switch (msgTypeId) {
                case CREATE_CACHE_MSG_ID -> handleCreateCache(buffer, index, cacheOpsPublisher);
                case ADD_CACHE_ENTRY_MSG_ID -> handleAddCacheEntry(buffer, index, cacheOpsPublisher);
                case GET_CACHE_ENTRY_MSG_ID -> handleGetCacheEntry(buffer, index, cacheOpsPublisher);
                case CLEAR_CACHE_MSG_ID -> handleClearCache(buffer, index, cacheOpsPublisher);
                case DELETE_CACHE_MSG_ID -> handleDeleteCache(buffer, index, cacheOpsPublisher);
                case GET_CACHE_ENTRIES_MSG_ID -> handleGetCacheEntries(buffer, index, cacheOpsPublisher);
                case SUBSCRIBE_TO_CACHE_MSG_ID -> handleSubscribeToCache(buffer, index, cacheOpsPublisher);
                case UNSUBSCRIBE_TO_CACHE_MSG_ID -> handleUnsubscribeToCache(buffer, index, cacheOpsPublisher);
                case GET_CACHE_STATS_MSG_ID -> handleGetCacheStats(buffer, index, cacheOpsPublisher);
                case REMOVE_CACHE_ENTRY_MSG_ID -> handleRemoveCacheEntry(buffer, index, cacheOpsPublisher);
                case BULK_OPS_MSG_ID -> handleBulkOps(buffer, index, cacheOpsPublisher);

                case CREATE_COUNTER_CACHE_MSG_ID -> handleCreateCache(buffer, index, counterOpsPublisher);
                case ADD_COUNTER_ENTRY_MSG_ID -> handleAddCounterCacheEntry(buffer, index, counterOpsPublisher);
                case GET_COUNTER_ENTRY_MSG_ID -> handleGetCacheEntry(buffer, index, counterOpsPublisher);
                case CLEAR_COUNTER_CACHE_MSG_ID -> handleClearCache(buffer, index, counterOpsPublisher);
                case DELETE_COUNTER_CACHE_MSG_ID -> handleDeleteCache(buffer, index, counterOpsPublisher);
                case GET_COUNTER_ENTRIES_MSG_ID -> handleGetCacheEntries(buffer, index, counterOpsPublisher);
                case SUBSCRIBE_TO_COUNTER_CACHE_MSG_ID -> handleSubscribeToCache(buffer, index, counterOpsPublisher);
                case UNSUBSCRIBE_TO_COUNTER_CACHE_MSG_ID -> handleUnsubscribeToCache(buffer, index, counterOpsPublisher);
                case GET_COUNTER_STATS_MSG_ID -> handleGetCacheStats(buffer, index, counterOpsPublisher);
                case REMOVE_COUNTER_ENTRY_MSG_ID -> handleRemoveCacheEntry(buffer, index, counterOpsPublisher);
                default -> log.warn("Got unknown msgType: {} processing inbound client cache requests", msgTypeId);
            }
        });
    }

    private <BV> void handleBulkOps(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
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
        cacheOpsPublisher.sendBulkOperationsRequest(requestId, request);
    }

    private <BV> void handleRemoveCacheEntry(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var key = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("REMOVE CACHE ENTRY Request has ID " + requestId + ", cache ID " + cacheId + ", remove key=" + key);
        cacheOpsPublisher.removeCacheEntry(requestId, cacheId, key);
    }

    private <BV> void handleGetCacheStats(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        log.debug("GET CACHE STATS Request has ID " + requestId);
        cacheOpsPublisher.getAllCacheStats(requestId);
    }

    private <BV> void handleUnsubscribeToCache(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("UNSUBSCRIBE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
        cacheOpsPublisher.sendCacheUnsubscribe(requestId, cacheId);
    }

    private <BV> void handleSubscribeToCache(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
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
        cacheOpsPublisher.sendCacheSubscribe(requestId, cacheIds, sendSnapshot);
    }

    private <BV> void handleGetCacheEntries(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("GET CACHE ENTRIES Request has ID " + requestId + ", on cache ID " + cacheId);
        cacheOpsPublisher.getCacheEntries(requestId, cacheId);
    }

    private <BV> void handleDeleteCache(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("DELETE CACHE Request has ID " + requestId + ", to delete cache ID " + cacheId);
        cacheOpsPublisher.deleteCache(requestId, cacheId);
    }

    private <BV> void handleClearCache(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        var cacheId = buffer.getStringUtf8(index + buffer.getInt(index) + 4);
        log.debug("CLEAR CACHE Request has ID " + requestId + ", to clear on cache ID " + cacheId);
        cacheOpsPublisher.clearCache(requestId, cacheId);
    }

    private <BV> void handleGetCacheEntry(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var key = buffer.getStringUtf8(cumulativeReadPosition);
        log.debug("GET CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", to get key=" + key);
        cacheOpsPublisher.getCacheEntry(requestId, cacheId, key);
    }

    private void handleAddCacheEntry(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, String> cacheOpsPublisher) {
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
        cacheOpsPublisher.addCacheEntry(requestId, cacheId, key, value, ttl);
    }

    private <BV> void handleCreateCache(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, BV> cacheOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        var cacheId = buffer.getStringUtf8(index + buffer.getInt(index) + 4);
        log.debug("CREATE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
        cacheOpsPublisher.sendCreateCache(requestId, cacheId);
    }

    private void handleAddCounterCacheEntry(MutableDirectBuffer buffer, int index, ClusterMessagePublisher<String, String, Long> counterOpsPublisher) {
        var requestId = buffer.getStringUtf8(index);
        var cumulativeReadPosition = index + (buffer.getInt(index) + 4);
        var cacheId = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var key = buffer.getStringUtf8(cumulativeReadPosition);
        cumulativeReadPosition += buffer.getInt(cumulativeReadPosition) + 4;
        var value = buffer.getLong(cumulativeReadPosition);
        cumulativeReadPosition+=8;
        var ttl = buffer.getLong(cumulativeReadPosition);
        log.debug("ADD CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", key=" + key + ", value=" + value+", ttl="+ttl);
        counterOpsPublisher.addCacheEntry(requestId, cacheId, key, value, ttl);
    }
}
