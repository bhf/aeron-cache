package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import java.nio.charset.StandardCharsets;

/**
 * Publish Aeron Counter requests into a {@link RingBuffer} to be processed by the
 * {@link org.agrona.concurrent.AgentRunner}.
 * Counter values are serialized as 8-byte longs rather than length-prefixed UTF-8 strings.
 */
@Log4j2
public class RBCountersRequestPublisher extends AbstractRBRequestPublisher<Long> {

    public RBCountersRequestPublisher(RingBuffer rb) {
        super(rb);
    }

    @Override protected int createCacheMsgId() { return CacheRequestMessageTypes.CREATE_COUNTER_CACHE_MSG_ID; }
    @Override protected int getCacheEntryMsgId() { return CacheRequestMessageTypes.GET_COUNTER_ENTRY_MSG_ID; }
    @Override protected int clearCacheMsgId() { return CacheRequestMessageTypes.CLEAR_COUNTER_CACHE_MSG_ID; }
    @Override protected int deleteCacheMsgId() { return CacheRequestMessageTypes.DELETE_COUNTER_CACHE_MSG_ID; }
    @Override protected int removeCacheEntryMsgId() { return CacheRequestMessageTypes.REMOVE_COUNTER_ENTRY_MSG_ID; }
    @Override protected int getCacheEntriesMsgId() { return CacheRequestMessageTypes.GET_COUNTER_ENTRIES_MSG_ID; }
    @Override protected int getCacheStatsMsgId() { return CacheRequestMessageTypes.GET_COUNTER_STATS_MSG_ID; }
    @Override protected int subscribeToCacheMsgId() { return CacheRequestMessageTypes.SUBSCRIBE_TO_COUNTER_CACHE_MSG_ID; }
    @Override protected int unsubscribeToCacheMsgId() { return CacheRequestMessageTypes.UNSUBSCRIBE_TO_COUNTER_CACHE_MSG_ID; }
    @Override protected int addCacheEntryMsgId() { return CacheRequestMessageTypes.ADD_COUNTER_ENTRY_MSG_ID; }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, Long value, long ttl) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        // Value is a Long: 8 bytes instead of length-prefixed string
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4) + 8 + 8;
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(addCacheEntryMsgId(), desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;

            buffer.putInt(writeCursor, requestIdBytes.length);
            writeCursor += 4;
            buffer.putBytes(writeCursor, requestIdBytes);
            writeCursor += requestIdBytes.length;

            buffer.putInt(writeCursor, cacheIdBytes.length);
            writeCursor += 4;
            buffer.putBytes(writeCursor, cacheIdBytes);
            writeCursor += cacheIdBytes.length;

            buffer.putInt(writeCursor, keyBytes.length);
            writeCursor += 4;
            buffer.putBytes(writeCursor, keyBytes);
            writeCursor += keyBytes.length;

            buffer.putLong(writeCursor, value);
            writeCursor += 8;

            buffer.putLong(writeCursor, ttl);
            writeCursor += 8;
            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write add counter entry to RingBuffer", e);
        }
    }

    @Override
    public void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request) {
        throw new UnsupportedOperationException("Bulk operations are not supported for counters");
    }

    @Override
    public void incrementCounter(String requestId, String cacheId, String key, long amount, long ttl) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        // amount and ttl are longs: 8 bytes each
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4) + 8 + 8;
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(CacheRequestMessageTypes.INCREMENT_COUNTER_ENTRY_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;

            buffer.putInt(writeCursor, requestIdBytes.length);
            writeCursor += 4;
            buffer.putBytes(writeCursor, requestIdBytes);
            writeCursor += requestIdBytes.length;

            buffer.putInt(writeCursor, cacheIdBytes.length);
            writeCursor += 4;
            buffer.putBytes(writeCursor, cacheIdBytes);
            writeCursor += cacheIdBytes.length;

            buffer.putInt(writeCursor, keyBytes.length);
            writeCursor += 4;
            buffer.putBytes(writeCursor, keyBytes);
            writeCursor += keyBytes.length;

            buffer.putLong(writeCursor, amount);
            writeCursor += 8;

            buffer.putLong(writeCursor, ttl);
            writeCursor += 8;
            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write increment counter entry to RingBuffer", e);
        }
    }
}