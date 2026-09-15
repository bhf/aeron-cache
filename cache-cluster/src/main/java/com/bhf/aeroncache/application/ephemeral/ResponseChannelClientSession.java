package com.bhf.aeroncache.application.ephemeral;

import io.aeron.DirectBufferVector;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;

/**
 * A per client {@link ClientSession} for the ephemeral cache, backed by a single Aeron
 * {@code control-mode=response} {@link Publication} routed back to the originating client.
 * <p>
 * Replaces the previous single fan-out session: because the cache service
 * ({@code CacheSubscriptionServiceImpl}) tracks subscribers and delivers responses by holding
 * {@link ClientSession} object references and calling {@link #offer} on each, giving every client
 * its own session instance makes request/response, subscription streaming and TTL-timer updates
 * route to the correct client with no changes to the service.
 * <p>
 * {@link #id()} is the request {@link Image#correlationId()} - stable and unique for the client's
 * lifetime, which the subscription service relies on for de-duplication and self-exclusion.
 */
public class ResponseChannelClientSession implements ClientSession {

    private final long correlationId;
    private final Image image;
    private final Publication responsePublication;

    public ResponseChannelClientSession(long correlationId, Image image, Publication responsePublication) {
        this.correlationId = correlationId;
        this.image = image;
        this.responsePublication = responsePublication;
    }

    @Override
    public long id() {
        return correlationId;
    }

    @Override
    public int responseStreamId() {
        return responsePublication.streamId();
    }

    @Override
    public String responseChannel() {
        return responsePublication.channel();
    }

    @Override
    public byte[] encodedPrincipal() {
        return new byte[0];
    }

    @Override
    public void close() {
        CloseHelper.quietClose(responsePublication);
    }

    @Override
    public boolean isClosing() {
        return image.isClosed();
    }

    @Override
    public long offer(DirectBuffer buffer, int offset, int length) {
        // If the client has gone (image closed) or its response publication is closed, report success
        // so the service's send loop (while (offer < 0) idle()) does not wedge the agent thread. A
        // merely not-yet-connected publication still returns a negative code so the loop keeps
        // retrying, which closes the fresh-start race where the first frame arrives before the
        // client's response subscription has finished connecting.
        if (image.isClosed() || responsePublication.isClosed()) {
            return length;
        }
        return responsePublication.offer(buffer, offset, length);
    }

    @Override
    public long offer(DirectBufferVector[] vectors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long tryClaim(int length, BufferClaim bufferClaim) {
        throw new UnsupportedOperationException();
    }
}
