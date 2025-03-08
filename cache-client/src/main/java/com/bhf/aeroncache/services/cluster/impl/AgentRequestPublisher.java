package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.messages.AddCacheEntryEncoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
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
    public void publishAddCachEntry(AeronCluster cluster, AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header) {
        rb.write(0, addCacheEntry.buffer(), 0, addCacheEntry.encodedLength()+header.encodedLength());
    }
}
