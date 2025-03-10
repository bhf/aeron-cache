package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.messages.*;
import io.aeron.cluster.client.AeronCluster;
import lombok.RequiredArgsConstructor;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

/**
 * Publish various cache requests onto a {@link org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer} to
 * be processed by an {@link org.agrona.concurrent.Agent} in an {@link org.agrona.concurrent.AgentRunner}.
 */
@RequiredArgsConstructor
public class AgentRequestPublisher extends ClusterMessagePublisher {

    final ManyToOneRingBuffer rb;

    @Override
    void publishAddCachEntry(AeronCluster cluster, AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header) {
        rb.write(0, addCacheEntry.buffer(), 0, addCacheEntry.encodedLength()+header.encodedLength());
    }

    @Override
    public void publishCreateCache(AeronCluster cluster, CreateCacheEncoder createCache, MessageHeaderEncoder header) {
        var msgLength = createCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(createCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, createCache.buffer(), 0, msgLength);
        rb.commit(index);
    }

    @Override
    void publishGetCacheEntry(AeronCluster cluster, GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header) {
        var msgLength = getCacheEntry.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(getCacheEntry.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, getCacheEntry.buffer(), 0, msgLength);
        rb.commit(index);
    }

    @Override
    void publishClearCache(AeronCluster cluster, ClearCacheEncoder clearCache, MessageHeaderEncoder header) {
        var msgLength = clearCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(clearCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, clearCache.buffer(), 0, msgLength);
        rb.commit(index);
    }

    @Override
    void publishDeleteCache(AeronCluster cluster, DeleteCacheEncoder deleteCache, MessageHeaderEncoder header) {
        var msgLength = deleteCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(deleteCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, deleteCache.buffer(), 0, msgLength);
        rb.commit(index);
    }

    @Override
    void publishRemoveCacheEntry(AeronCluster cluster, RemoveCacheEntryEncoder removeCacheEntry, MessageHeaderEncoder header) {
        var msgLength = removeCacheEntry.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(removeCacheEntry.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, removeCacheEntry.buffer(), 0, msgLength);
        rb.commit(index);
    }
}
