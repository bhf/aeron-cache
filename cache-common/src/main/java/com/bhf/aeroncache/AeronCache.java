package com.bhf.aeroncache;

import org.agrona.MutableDirectBuffer;

/**
 * Abstract out operations being done on {@link io.aeron.cluster.client.AeronCluster}
 * instances in order to support non-clustered caches.
 */
public interface AeronCache extends AutoCloseable {

    void sendKeepAlive();

    int pollEgress();

    long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int i);

    boolean isConnected();

    @Override
    default void close() {
        // Default no-op
    }
}
