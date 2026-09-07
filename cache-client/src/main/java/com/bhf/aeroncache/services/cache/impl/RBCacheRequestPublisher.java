package com.bhf.aeroncache.services.cache.impl;


import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import java.nio.charset.StandardCharsets;

/**
 * Publish Aeron Cache requests into a {@link RingBuffer} to be processed by the
 * {@link org.agrona.concurrent.AgentRunner}.
 */
@Log4j2
public class RBCacheRequestPublisher extends AbstractRBRequestPublisher<String> {

    private final MutableDirectBuffer writeBuffer = new ExpandableArrayBuffer(4096);

    public RBCacheRequestPublisher(RingBuffer rb) {
        super(rb);
    }

    @Override protected int createCacheMsgId() { return CacheRequestMessageTypes.CREATE_CACHE_MSG_ID; }
    @Override protected int getCacheEntryMsgId() { return CacheRequestMessageTypes.GET_CACHE_ENTRY_MSG_ID; }
    @Override protected int clearCacheMsgId() { return CacheRequestMessageTypes.CLEAR_CACHE_MSG_ID; }
    @Override protected int deleteCacheMsgId() { return CacheRequestMessageTypes.DELETE_CACHE_MSG_ID; }
    @Override protected int removeCacheEntryMsgId() { return CacheRequestMessageTypes.REMOVE_CACHE_ENTRY_MSG_ID; }
    @Override protected int getCacheEntriesMsgId() { return CacheRequestMessageTypes.GET_CACHE_ENTRIES_MSG_ID; }
    @Override protected int getCacheStatsMsgId() { return CacheRequestMessageTypes.GET_CACHE_STATS_MSG_ID; }
    @Override protected int subscribeToCacheMsgId() { return CacheRequestMessageTypes.SUBSCRIBE_TO_CACHE_MSG_ID; }
    @Override protected int unsubscribeToCacheMsgId() { return CacheRequestMessageTypes.UNSUBSCRIBE_TO_CACHE_MSG_ID; }
    @Override protected int addCacheEntryMsgId() { return CacheRequestMessageTypes.ADD_CACHE_ENTRY_MSG_ID; }
    @Override protected int patchValueMsgId() { return CacheRequestMessageTypes.PATCH_CACHE_ENTRY_MSG_ID; }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, long ttl) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4) + (valueBytes.length + 4) + 8;
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

            buffer.putInt(writeCursor, valueBytes.length);
            writeCursor += 4;
            buffer.putBytes(writeCursor, valueBytes);
            writeCursor += valueBytes.length;

            buffer.putLong(writeCursor, ttl);
            writeCursor += 8;
            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write add cache entry to RingBuffer", e);
        }
    }

    @Override
    public void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request) {

        var buffer = writeBuffer;
        int writeCursor = 0;
        writeCursor += buffer.putStringUtf8(writeCursor, requestId);

        buffer.putInt(writeCursor, request.operations().size());
        writeCursor += 4;

        for (int i = 0; i < request.operations().size(); i++) {
            var op = request.operations().get(i);
            var opRequestId = op.requestId();
            var cacheId = op.cacheId();
            var key = op.key();
            var value = op.value();
            var ttl = op.ttl();
            var counterValue = op.counterValue();
            var opType = op.operationType();

            writeCursor += buffer.putStringUtf8(writeCursor, opRequestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            writeCursor += buffer.putStringUtf8(writeCursor, key);
            writeCursor += buffer.putStringUtf8(writeCursor, value);

            buffer.putLong(writeCursor, ttl);
            writeCursor += 8;

            buffer.putLong(writeCursor, counterValue);
            writeCursor += 8;

            buffer.putInt(writeCursor, opType.ordinal());
            writeCursor += 4;
        }

        var length = writeCursor;
        while(!rb.write(CacheRequestMessageTypes.BULK_OPS_MSG_ID, writeBuffer, 0, length)){
            
        }
    }
}
