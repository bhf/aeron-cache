package com.bhf.aeroncache.application.ephemeral;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.cluster.service.ClientSession;
import lombok.extern.log4j.Log4j2;
import org.agrona.collections.Long2ObjectHashMap;

import java.util.Collection;

/**
 * Tracks the per client {@link ResponseChannelClientSession} used to send responses and streaming
 * updates back to an ephemeral cache client.
 * <p>
 * Uses Aeron response channels ({@code control-mode=response}): the response publication for a
 * session is created from the request {@link Image}'s {@code correlationId()}, which Aeron uses to
 * route the publication back to the originating client. No application level handshake is required.
 * Mirrors {@code GatewaySessionRegistry}.
 * <p>
 * Accessed only from the ephemeral cache agent thread.
 */
@Log4j2
public class EphemeralSessionRegistry {

    private final Aeron aeron;
    private final ChannelUriStringBuilder responseUriBuilder;
    private final int responseStreamId;
    private final Long2ObjectHashMap<ResponseChannelClientSession> sessions = new Long2ObjectHashMap<>();

    public EphemeralSessionRegistry(Aeron aeron, String responseControlEndpoint, int responseStreamId) {
        this.aeron = aeron;
        this.responseStreamId = responseStreamId;
        this.responseUriBuilder = new ChannelUriStringBuilder()
                .media("udp")
                .controlMode("response")
                .controlEndpoint(responseControlEndpoint);
    }

    /**
     * Get the session for the given request image, creating it (and its routed response publication)
     * on first use.
     *
     * @param image the request subscription image for the client.
     * @return the session whose response publication routes back to that client.
     */
    public ClientSession getOrCreate(Image image) {
        final long correlationId = image.correlationId();
        ResponseChannelClientSession session = sessions.get(correlationId);
        if (session == null) {
            final String channel = responseUriBuilder.responseCorrelationId(correlationId).build();
            final Publication publication = aeron.addPublication(channel, responseStreamId);
            session = new ResponseChannelClientSession(correlationId, image, publication);
            sessions.put(correlationId, session);
            log.info("Created response publication for ephemeral session {}", correlationId);
        }
        return session;
    }

    /**
     * @param correlationId the image correlation id identifying the session.
     * @return the session, or {@code null} if none exists.
     */
    public ClientSession get(long correlationId) {
        return sessions.get(correlationId);
    }

    /**
     * @return a live view of the current sessions.
     */
    public Collection<ClientSession> values() {
        return new java.util.ArrayList<>(sessions.values());
    }

    /**
     * Remove and close the session for the given correlation id.
     *
     * @param correlationId the image correlation id identifying the session.
     */
    public void remove(long correlationId) {
        final ResponseChannelClientSession session = sessions.remove(correlationId);
        if (session != null) {
            log.info("Closing response publication for ephemeral session {}", correlationId);
            session.close();
        }
    }

    /**
     * Close all sessions.
     */
    public void close() {
        sessions.values().forEach(ResponseChannelClientSession::close);
        sessions.clear();
    }
}
