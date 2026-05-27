package com.bhf.aeroncache.services.cache.impl;


import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;

/**
 * Publish Aeron Cache requests into a {@link RingBuffer} to be processed by the
 * {@link org.agrona.concurrent.AgentRunner}.
 */
@RequiredArgsConstructor
@Log4j2
public class RBCacheRequestPublisher implements CacheRequestPublisher {

    final RingBuffer rb;
    private final MutableDirectBuffer bulkOpsBuffer = new ExpandableArrayBuffer();

    @Override
    public void sendCreateCache(String requestId, String cacheId) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.CREATE_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write send create cache request to RingBuffer", e);
        }
    }

    @Override
    public void addCacheEntry(String requestId, String cacheId, String key, String value, long ttl) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4) + (key.length() + 4) + (value.length() + 4) + 8;
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.ADD_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            writeCursor += buffer.putStringUtf8(writeCursor, key);
            writeCursor += buffer.putStringUtf8(writeCursor, value);
            buffer.putLong(writeCursor, ttl);
            writeCursor +=8;
            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write add cache entry to RingBuffer", e);
        }
    }

    @Override
    public void getCacheEntry(String requestId, String cacheId, String key) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4) + (key.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.GET_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            writeCursor += buffer.putStringUtf8(writeCursor, key);
            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache entry to RingBuffer", e);
        }
    }

    @Override
    public void clearCache(String requestId, String cacheId) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.CLEAR_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write clear cache request to RingBuffer", e);
        }
    }

    @Override
    public void deleteCache(String requestId, String cacheId) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.DELETE_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write delete cache request to RingBuffer", e);
        }
    }


    @Override
    public void removeCacheEntry(String requestId, String cacheId, String key) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4) + (key.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.REMOVE_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            writeCursor += buffer.putStringUtf8(writeCursor, key);
            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write remove cache entry to RingBuffer", e);
        }
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.GET_CACHE_ENTRIES_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache entries request to RingBuffer", e);
        }
    }

    @Override
    public void getAllCacheStats(String requestId) {
        var desiredLength = (requestId.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.GET_CACHE_STATS_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache stats to RingBuffer", e);
        }
    }

    @Override
    public void sendCacheSubscribe(String requestId, String cacheId) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.SUBSCRIBE_TO_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write cache subscribe request to RingBuffer", e);
        }
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId) {
        var desiredLength = (requestId.length() + 4) + (cacheId.length() + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(com.bhf.aeroncache.models.CacheRequestMessageTypes.UNSUBSCRIBE_TO_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int writeCursor = claimIndex;
            writeCursor += buffer.putStringUtf8(writeCursor, requestId);
            writeCursor += buffer.putStringUtf8(writeCursor, cacheId);
            log.trace("TOTAL WRITTEN=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write cache unsubscribe request to RingBuffer", e);
        }
    }

    @Override
    public void sendBulkOperationsRequest(String requestId, BulkCacheOpsRequest request) {

        var buffer = bulkOpsBuffer;
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
        while(!rb.write(CacheRequestMessageTypes.BULK_OPS_MSG_ID, bulkOpsBuffer, 0, length)){
            
        }
    }
}
