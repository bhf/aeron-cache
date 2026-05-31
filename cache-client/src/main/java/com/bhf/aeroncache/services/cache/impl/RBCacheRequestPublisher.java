package com.bhf.aeroncache.services.cache.impl;


import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.BufferUtil;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import java.nio.charset.StandardCharsets;

/**
 * Publish Aeron Cache requests into a {@link RingBuffer} to be processed by the
 * {@link org.agrona.concurrent.AgentRunner}.
 */
@RequiredArgsConstructor
@Log4j2
public class RBCacheRequestPublisher implements CacheRequestPublisher {

    final RingBuffer rb;
    private final MutableDirectBuffer writeBuffer = new ExpandableArrayBuffer(4096);

    @Override
    public void sendCreateCache(String requestId, String cacheId) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.CREATE_CACHE_MSG_ID, desiredLength)) < 0) {
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

            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write send create cache request to RingBuffer", e);
        }
    }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, long ttl) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4) + (valueBytes.length + 4) + 8;
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.ADD_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
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
    public void getCacheEntry(String requestId, String cacheId, String key) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.GET_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
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

            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache entry to RingBuffer", e);
        }
    }

    @Override
    public void clearCache(String requestId, String cacheId) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.CLEAR_CACHE_MSG_ID, desiredLength)) < 0) {
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

            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write clear cache request to RingBuffer", e);
        }
    }

    @Override
    public void deleteCache(String requestId, String cacheId) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.DELETE_CACHE_MSG_ID, desiredLength)) < 0) {
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

            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write delete cache request to RingBuffer", e);
        }
    }


    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.REMOVE_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
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

            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write remove cache entry to RingBuffer", e);
        }
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.GET_CACHE_ENTRIES_MSG_ID, desiredLength)) < 0) {
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

            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache entries request to RingBuffer", e);
        }
    }

    @Override
    public void getAllCacheStats(String requestId) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.GET_CACHE_STATS_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;

            buffer.putInt(writeCursor, requestIdBytes.length);
            writeCursor += 4;
            buffer.putBytes(writeCursor, requestIdBytes);
            writeCursor += requestIdBytes.length;

            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache stats to RingBuffer", e);
        }
    }

    @Override
    public void sendCacheSubscribe(String requestId, String cacheId, boolean sendSnapshot) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + 1;
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.SUBSCRIBE_TO_CACHE_MSG_ID, desiredLength)) < 0) {
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

            buffer.putByte(writeCursor, sendSnapshot ? (byte)1 : (byte)0);
            writeCursor+=1;
            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write cache subscribe request to RingBuffer", e);
        }
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId) {

        if(requestId==null || cacheId==null){
            throw new NullPointerException();
        }

        byte[] requestIdBytes = requestId != null ? requestId.getBytes(StandardCharsets.UTF_8) : BufferUtil.NULL_BYTES;
        byte[] cacheIdBytes = cacheId != null ? cacheId.getBytes(StandardCharsets.UTF_8) : BufferUtil.NULL_BYTES;

        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.UNSUBSCRIBE_TO_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;

            buffer.putInt(writeCursor, requestIdBytes.length);
            writeCursor+=4;
            buffer.putBytes(writeCursor, requestIdBytes);
            writeCursor+=requestIdBytes.length;

            buffer.putInt(writeCursor, cacheIdBytes.length);
            writeCursor+=4;
            buffer.putBytes(writeCursor, cacheIdBytes);
            writeCursor+=cacheIdBytes.length;

            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write cache unsubscribe request to RingBuffer", e);
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
            var opType = op.operationType();

            writeCursor += buffer.putStringUtf8(writeCursor, opRequestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            writeCursor += buffer.putStringUtf8(writeCursor, key);
            writeCursor += buffer.putStringUtf8(writeCursor, value);

            buffer.putLong(writeCursor, ttl);
            writeCursor += 8;

            buffer.putInt(writeCursor, opType.ordinal());
            writeCursor += 4;
        }

        var length = writeCursor;
        while(!rb.write(CacheRequestMessageTypes.BULK_OPS_MSG_ID, writeBuffer, 0, length)){
            
        }
    }
}
