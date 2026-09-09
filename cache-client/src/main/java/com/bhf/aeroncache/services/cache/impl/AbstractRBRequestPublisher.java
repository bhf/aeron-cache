package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Base class for publishing Aeron Cache requests into a {@link RingBuffer}.
 *
 * @param <BV> The base value type.
 */
@RequiredArgsConstructor
@Log4j2
public abstract class AbstractRBRequestPublisher<BV> implements CacheRequestPublisher<String, String, BV> {

    final RingBuffer rb;
    private final MutableDirectBuffer writeBuffer = new ExpandableArrayBuffer(4096);

    protected abstract int createCacheMsgId();
    protected abstract int getCacheEntryMsgId();
    protected abstract int clearCacheMsgId();
    protected abstract int deleteCacheMsgId();
    protected abstract int removeCacheEntryMsgId();
    protected abstract int getCacheEntriesMsgId();
    protected abstract int getCacheStatsMsgId();
    protected abstract int subscribeToCacheMsgId();
    protected abstract int unsubscribeToCacheMsgId();
    protected abstract int addCacheEntryMsgId();
    protected abstract int patchValueMsgId();
    protected abstract int cancelItemRemovalMsgId();

    @Override
    public void sendCreateCache(String requestId, String cacheId) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(createCacheMsgId(), desiredLength)) < 0) {
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
    public void getCacheEntry(String requestId, String cacheId, String key) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(getCacheEntryMsgId(), desiredLength)) < 0) {
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
        while ((claimIndex = rb.tryClaim(clearCacheMsgId(), desiredLength)) < 0) {
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
        while ((claimIndex = rb.tryClaim(deleteCacheMsgId(), desiredLength)) < 0) {
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
        while ((claimIndex = rb.tryClaim(removeCacheEntryMsgId(), desiredLength)) < 0) {
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
    public void cancelItemRemoval(String requestId, String cacheId, String key) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(cancelItemRemovalMsgId(), desiredLength)) < 0) {
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
            log.error("Error whilst trying to write cancel item removal to RingBuffer", e);
        }
    }

    @Override
    public void patchValue(String requestId, String cacheId, String key, BV value) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4) + (keyBytes.length + 4) + (valueBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(patchValueMsgId(), desiredLength)) < 0) {
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

            log.trace("TOTAL WRITTEN BYTES=" + (writeCursor - claimIndex));
            rb.commit(claimIndex);
        } catch (Exception e) {
            rb.abort(claimIndex);
            log.error("Error whilst trying to write patch value request to RingBuffer", e);
        }
    }

    @Override
    public void getCacheEntries(String requestId, String cacheId) {
        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);
        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(getCacheEntriesMsgId(), desiredLength)) < 0) {
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
        while ((claimIndex = rb.tryClaim(getCacheStatsMsgId(), desiredLength)) < 0) {
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
    public void sendCacheSubscribe(String requestId, List<String> cacheId, boolean sendSnapshot) {
        sendCacheSubscribe(requestId, cacheId, null, null, sendSnapshot);
    }

    @Override
    public void sendCacheSubscribe(String requestId, List<String> cacheId, List<String> keys, List<SubscriptionMode> modes, boolean sendSnapshot) {

        if (requestId == null) {
            return;
        }

        var buffer = writeBuffer;
        int writeCursor = 0;
        writeCursor += buffer.putStringUtf8(writeCursor, requestId);

        buffer.putInt(writeCursor, cacheId.size());
        writeCursor += 4;

        for (String id : cacheId) {
            writeCursor += buffer.putStringUtf8(writeCursor, id);
        }

        buffer.putByte(writeCursor, sendSnapshot ? (byte) 1 : (byte) 0);
        writeCursor += 1;

        for (int i = 0; i < cacheId.size(); i++) {
            String key = (keys == null) ? null : keys.get(i);
            writeCursor += buffer.putStringUtf8(writeCursor, key == null ? "" : key);
        }

        for (int i = 0; i < cacheId.size(); i++) {
            SubscriptionMode mode = (modes == null) ? SubscriptionMode.FULL : modes.get(i);
            buffer.putByte(writeCursor, mode == SubscriptionMode.PATCH ? (byte) 1 : (byte) 0);
            writeCursor += 1;
        }

        var length = writeCursor;
        while (!rb.write(subscribeToCacheMsgId(), writeBuffer, 0, length)) {
        }
    }

    @Override
    public void sendCacheUnsubscribe(String requestId, String cacheId) {

        if (requestId == null || cacheId == null) {
            throw new NullPointerException();
        }

        byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
        byte[] cacheIdBytes = cacheId.getBytes(StandardCharsets.UTF_8);

        var desiredLength = (requestIdBytes.length + 4) + (cacheIdBytes.length + 4);
        log.trace("DESIRED LENGTH=" + desiredLength);

        var claimIndex = -1;
        while ((claimIndex = rb.tryClaim(unsubscribeToCacheMsgId(), desiredLength)) < 0) {
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
            log.error("Error whilst trying to write cache unsubscribe request to RingBuffer", e);
        }
    }
}
