package com.bhf.aeroncache.utils;

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

/**
 * Utilities and helpers for creating Ringbuffers.
 */
public class RingBufferUtils {

    public static ManyToOneRingBuffer buildRingbuffer(int size) {
        var bufferSize = size+ RingBufferDescriptor.TRAILER_LENGTH;
        AtomicBuffer buffer = new UnsafeBuffer(new ExpandableDirectByteBuffer(bufferSize));
        return new ManyToOneRingBuffer(buffer);
    }
}
