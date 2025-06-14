package com.bhf.aeroncache.services.cluster;

import io.aeron.DirectBufferVector;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class TestUtils {

    /**
     * The mocked session copies response data over to the
     * response buffer.
     *
     * @return The mocked client session.
     */
    public static ClientSession getMockedSession(MutableDirectBuffer responseBuffer) {
        return new ClientSession() {
            @Override
            public long id() {
                return 0;
            }

            @Override
            public int responseStreamId() {
                return 0;
            }

            @Override
            public String responseChannel() {
                return null;
            }

            @Override
            public byte[] encodedPrincipal() {
                return new byte[0];
            }

            @Override
            public void close() {

            }

            @Override
            public boolean isClosing() {
                return false;
            }

            @Override
            public long offer(DirectBuffer buffer, int offset, int length) {
                responseBuffer.putBytes(0, buffer, offset, length);
                return length;
            }

            @Override
            public long offer(DirectBufferVector[] vectors) {
                return 0;
            }

            @Override
            public long tryClaim(int length, BufferClaim bufferClaim) {
                return 0;
            }
        };
    }
}
