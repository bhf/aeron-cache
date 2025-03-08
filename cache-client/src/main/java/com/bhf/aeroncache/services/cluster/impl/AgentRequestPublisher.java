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
        rb.write(0, createCache.buffer(), 0, createCache.encodedLength()+header.encodedLength());
    }

    @Override
    void publishGetCacheEntry(AeronCluster cluster, GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header) {
        rb.write(0, getCacheEntry.buffer(), 0, getCacheEntry.encodedLength()+header.encodedLength());
    }

    @Override
    void publishClearCache(AeronCluster cluster, ClearCacheEncoder clearCache, MessageHeaderEncoder header) {
        rb.write(0, clearCache.buffer(), 0, clearCache.encodedLength()+header.encodedLength());
    }

    @Override
    void publishDeleteCache(AeronCluster cluster, DeleteCacheEncoder deleteCache, MessageHeaderEncoder header) {
        rb.write(0, deleteCache.buffer(), 0, deleteCache.encodedLength()+header.encodedLength());
    }

    @Override
    void publishRemoveCacheEntry(AeronCluster cluster, RemoveCacheEntryEncoder removeCacheEntry, MessageHeaderEncoder header) {
        rb.write(0, removeCacheEntry.buffer(), 0, removeCacheEntry.encodedLength()+header.encodedLength());
    }
}
