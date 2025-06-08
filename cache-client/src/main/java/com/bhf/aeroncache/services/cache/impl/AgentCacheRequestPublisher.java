package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.utils.RingBufferUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import java.util.concurrent.Executors;

import static com.bhf.aeroncache.services.cache.impl.CacheRequestMessageTypes.*;

/**
 * Publish Aeron Cache requests into a {@link RingBuffer} to be processed by the
 * {@link org.agrona.concurrent.AgentRunner}.
 */
@RequiredArgsConstructor
@Log4j2
public class AgentCacheRequestPublisher implements CacheRequestPublisher {

    public static void main(String[] args) {
        RingBuffer rb = RingBufferUtils.buildRingbuffer(1024);
        AgentCacheRequestPublisher publisher = new AgentCacheRequestPublisher(rb);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                int requestCount = 0;
                long cacheId = 0L;
                while (true) {
                    cacheId++;
                    String key = "someKey-" + cacheId + "-" + requestCount;
                    String value = "someValue-" + cacheId + "-" + requestCount;
                    publisher.sendCreateCache("requestId-" + requestCount, cacheId);
                    requestCount++;
                    publisher.addCacheEntry("requestId-" + requestCount, cacheId, key, value);
                    requestCount++;
                    publisher.getCacheEntry("requestId-" + requestCount, cacheId, key);
                    requestCount++;
                    publisher.clearCache("requestId-" + requestCount, cacheId);
                    requestCount++;
                    publisher.deleteCache("requestId-" + requestCount, cacheId);
                    requestCount++;
                    publisher.getCacheEntries("requestId-" + requestCount, cacheId);
                    requestCount++;
                    publisher.sendCacheSubscribe("requestId-" + requestCount, cacheId);
                    requestCount++;
                    publisher.sendCacheUnsubscribe("requestId-" + requestCount, cacheId);
                    requestCount++;
                    publisher.getAllCacheStats("requestId-" + requestCount);
                    requestCount++;
                    publisher.removeCacheEntry("requestId-"+requestCount, cacheId, key);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        });


        while (true) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            rb.read(new MessageHandler() {
                @Override
                public void onMessage(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length) {
                    log.info("Got msg ID " + msgTypeId + " at index " + index + ", length=" + length);

                    if (msgTypeId == CREATE_CACHE_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cacheId = buffer.getLong(index + requestId.length() + 4);
                        log.info("CREATE CACHE Request has ID " + requestId + ", on cache ID " + cacheId);
                    }
                    if (msgTypeId == ADD_CACHE_ENTRY_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cumulativeReadPosition = index + (requestId.length() + 4);
                        var cacheId = buffer.getLong(cumulativeReadPosition);
                        cumulativeReadPosition += 8;
                        var key = buffer.getStringUtf8(cumulativeReadPosition);
                        cumulativeReadPosition += key.length() + 4;
                        var value = buffer.getStringUtf8(cumulativeReadPosition);
                        log.info("ADD CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", key=" + key + ", value=" + value);
                    }
                    if (msgTypeId == GET_CACHE_ENTRY_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cumulativeReadPosition = index + (requestId.length() + 4);
                        var cacheId = buffer.getLong(cumulativeReadPosition);
                        cumulativeReadPosition += 8;
                        var key = buffer.getStringUtf8(cumulativeReadPosition);
                        log.info("GET CACHE ENTRY Request has ID " + requestId + ", on cache ID " + cacheId + ", to get key=" + key);
                    }
                    if (msgTypeId == CLEAR_CACHE_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cacheId = buffer.getLong(index + requestId.length() + 4);
                        log.info("CLEAR CACHE Request has ID " + requestId + ", to clear on cache ID " + cacheId);
                    }
                    if (msgTypeId == DELETE_CACHE_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cumulativeReadPosition = index + (requestId.length() + 4);
                        var cacheId = buffer.getLong(cumulativeReadPosition);
                        log.info("DELETE CACHE Request has ID "+requestId+", to delete cache ID "+cacheId);
                    }
                    if (msgTypeId == GET_CACHE_ENTRIES_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cumulativeReadPosition = index + (requestId.length() + 4);
                        var cacheId = buffer.getLong(cumulativeReadPosition);
                        log.info("GET CACHE ENTRIES Request has ID "+requestId+", on cache ID "+cacheId);
                    }
                    if (msgTypeId == SUBSCRIBE_TO_CACHE_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cumulativeReadPosition = index + (requestId.length() + 4);
                        var cacheId = buffer.getLong(cumulativeReadPosition);
                        log.info("SUBSCRIBE CACHE Request has ID "+requestId+", on cache ID "+cacheId);
                    }
                    if (msgTypeId == UNSUBSCRIBE_TO_CACHE_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cumulativeReadPosition = index + (requestId.length() + 4);
                        var cacheId = buffer.getLong(cumulativeReadPosition);
                        log.info("UNSUBSCRIBE CACHE Request has ID "+requestId+", on cache ID "+cacheId);
                    }
                    if (msgTypeId == GET_CACHE_STATS_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        log.info("GET CACHE STATS Request has ID "+requestId);
                    }
                    if (msgTypeId == REMOVE_CACHE_ENTRY_MSG_ID) {
                        var requestId = buffer.getStringUtf8(index);
                        var cumulativeReadPosition = index + (requestId.length() + 4);
                        var cacheId = buffer.getLong(cumulativeReadPosition);
                        cumulativeReadPosition += 8;
                        var key = buffer.getStringUtf8(cumulativeReadPosition);
                        log.info("REMOVE CACHE ENTRY Request has ID " + requestId + ", cache ID " + cacheId + ", remove key=" + key);
                    }
                }
            });

        }
    }

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
