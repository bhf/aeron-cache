package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.AeronCache;
import lombok.RequiredArgsConstructor;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

/**
 * Publish various pre-encoded cache requests onto a {@link org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer} to
 * be processed by an {@link org.agrona.concurrent.Agent} in an {@link org.agrona.concurrent.AgentRunner}.
 */
@RequiredArgsConstructor
public class AgentClusterMessagePublisher extends ClusterMessagePublisher {

    final ManyToOneRingBuffer rb;

    @Override
    void publishAddCachEntry(AeronCache cluster, AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = addCacheEntry.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(addCacheEntry.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, addCacheEntry.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    public void publishCreateCache(AeronCache cluster, CreateCacheEncoder createCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = createCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(createCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, createCache.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishGetCacheEntry(AeronCache cluster, GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = getCacheEntry.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(getCacheEntry.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, getCacheEntry.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishClearCache(AeronCache cluster, ClearCacheEncoder clearCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = clearCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(clearCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, clearCache.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishDeleteCache(AeronCache cluster, DeleteCacheEncoder deleteCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = deleteCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(deleteCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, deleteCache.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishRemoveCacheEntry(AeronCache cluster, RemoveCacheEntryEncoder removeCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = removeCacheEntry.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(removeCacheEntry.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, removeCacheEntry.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }
}
