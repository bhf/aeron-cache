package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import static com.bhf.aeroncache.model.CacheRequestMessageTypes.*;

/**
 * Publish Aeron Cache requests into a {@link RingBuffer} to be processed by the
 * {@link org.agrona.concurrent.AgentRunner}.
 */
@RequiredArgsConstructor
@Log4j2
public class RBCacheRequestPublisher implements CacheRequestPublisher {

    final RingBuffer rb;

    @Override
    public void sendCreateCache(String requestId, long cacheId) {
        var desiredLength = (requestId.length() + 4) + 8;
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(CREATE_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            log.info("TOTAL WRITTEN=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write send create cache request to RingBuffer", e);
        }
    }

    @Override
    public void addCacheEntry(String requestId, long cacheId, String key, String value) {
        var desiredLength = (requestId.length() + 4) + 8 + (key.length() + 4) + (value.length() + 4);
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(ADD_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, key);
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, value);
            log.info("TOTAL WRITTEN BYTES=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write add cache entry to RingBuffer", e);
        }
    }

    @Override
    public void getCacheEntry(String requestId, long cacheId, String key) {
        var desiredLength = (requestId.length() + 4) + 8 + (key.length() + 4);
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(GET_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, key);
            log.info("TOTAL WRITTEN BYTES=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache entry to RingBuffer", e);
        }
    }

    @Override
    public void clearCache(String requestId, long cacheId) {
        var desiredLength = (requestId.length() + 4) + 8;
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(CLEAR_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            log.info("TOTAL WRITTEN=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write clear cache request to RingBuffer", e);
        }
    }

    @Override
    public void deleteCache(String requestId, long cacheId) {
        var desiredLength = (requestId.length() + 4) + 8;
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(DELETE_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            log.info("TOTAL WRITTEN=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write delete cache request to RingBuffer", e);
        }
    }


    @Override
    public void removeCacheEntry(String requestId, long cacheId, String key) {
        var desiredLength = (requestId.length() + 4) + 8 + (key.length() + 4);
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(REMOVE_CACHE_ENTRY_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, key);
            log.info("TOTAL WRITTEN BYTES=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write remove cache entry to RingBuffer", e);
        }
    }

    @Override
    public void getCacheEntries(String requestId, long cacheId) {
        var desiredLength = (requestId.length() + 4) + 8;
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(GET_CACHE_ENTRIES_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            log.info("TOTAL WRITTEN=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache entries request to RingBuffer", e);
        }
    }

    @Override
    public void getAllCacheStats(String requestId) {
        var desiredLength = (requestId.length() + 4);
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(GET_CACHE_STATS_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            log.info("TOTAL WRITTEN=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write get cache stats to RingBuffer", e);
        }
    }

    @Override
    public void sendCacheSubscribe(String requestId, long cacheId) {
        var desiredLength = (requestId.length() + 4) + 8;
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(SUBSCRIBE_TO_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            log.info("TOTAL WRITTEN=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write cache subscribe request to RingBuffer", e);
        }
    }

    public void sendCacheUnsubscribe(String requestId, long cacheId) {
        var desiredLength = (requestId.length() + 4) + 8;
        log.info("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(UNSUBSCRIBE_TO_CACHE_MSG_ID, desiredLength)) < 0) {
        }

        try {
            var buffer = rb.buffer();
            int cumulativeWritePosition = claimIndex;
            cumulativeWritePosition += buffer.putStringUtf8(cumulativeWritePosition, requestId);
            buffer.putLong(cumulativeWritePosition, cacheId);
            cumulativeWritePosition += 8;
            log.info("TOTAL WRITTEN=" + (cumulativeWritePosition - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write cache unsubscribe request to RingBuffer", e);
        }
    }
}
