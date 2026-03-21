package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.codecs.RegularStringCacheRequestEncoder;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

/**
 * Publish various pre-encoded cache requests onto a {@link org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer} to
 * be processed by an {@link org.agrona.concurrent.Agent} in an {@link org.agrona.concurrent.AgentRunner}.
 */


public class RBClusterMessagePublisher extends ClusterMessagePublisher {

    final ManyToOneRingBuffer rb;

    public RBClusterMessagePublisher(AeronCache cluster, ManyToOneRingBuffer rb) {
        super(cluster, new BusySpinIdleStrategy(), new RegularStringCacheRequestEncoder());
        this.rb = rb;
    }

    @Override
    public void publishToCache(MutableDirectBuffer msgBuffer, int offset, int length) {
        var index = rb.tryClaim(8, length);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, msgBuffer, offset, length);
        rb.commit(index);
    }
}
