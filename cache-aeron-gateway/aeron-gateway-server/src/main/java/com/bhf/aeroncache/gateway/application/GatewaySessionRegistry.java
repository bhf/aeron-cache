package com.bhf.aeroncache.gateway.application;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.Image;
import io.aeron.Publication;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.collections.Long2ObjectHashMap;

/**
 * Tracks the per client session Aeron response {@link Publication} used to send
 * command responses and streaming updates back to a gateway client.
 * <p>
 * Uses Aeron response channels ({@code control-mode=response}): the response
 * publication for a session is created from the request {@link Image}'s
 * {@code correlationId()}, which Aeron uses to route the publication back to the
 * originating client. No application level handshake is required.
 * <p>
 * Accessed only from the gateway ingress agent thread.
 */
@Log4j2
public class GatewaySessionRegistry {

    private final Aeron aeron;
    private final ChannelUriStringBuilder responseUriBuilder;
    private final int responseStreamId;
    private final Long2ObjectHashMap<Publication> sessions = new Long2ObjectHashMap<>();

    public GatewaySessionRegistry(Aeron aeron, String responseControlEndpoint, int responseStreamId) {
        this.aeron = aeron;
        this.responseStreamId = responseStreamId;
        this.responseUriBuilder = new ChannelUriStringBuilder()
                .media("udp")
                .controlMode("response")
                .controlEndpoint(responseControlEndpoint);
    }

    /**
     * Get the response publication for the given request image, creating it on first use.
     *
     * @param image The request subscription image for the client.
     * @return The response publication routed back to that client.
     */
    public Publication getOrCreate(Image image) {
        final long correlationId = image.correlationId();
        Publication publication = sessions.get(correlationId);
        if (publication == null) {
            final String channel = responseUriBuilder.responseCorrelationId(correlationId).build();
            publication = aeron.addPublication(channel, responseStreamId);
            sessions.put(correlationId, publication);
            log.info("Created response publication for gateway session {}", correlationId);
        }
        return publication;
    }

    /**
     * Remove and close the response publication for a session.
     *
     * @param correlationId The image correlation id identifying the session.
     */
    public void remove(long correlationId) {
        final Publication publication = sessions.remove(correlationId);
        if (publication != null) {
            log.info("Closing response publication for gateway session {}", correlationId);
            CloseHelper.quietClose(publication);
        }
    }

    /**
     * Close all response publications.
     */
    public void close() {
        sessions.values().forEach(CloseHelper::quietClose);
        sessions.clear();
    }
}
