package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.AeronCache;
import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.ExclusivePublication;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;

/**
 * Builds an {@link AeronCache} client that talks to the ephemeral (unclustered) cache over Aeron
 * response channels ({@code control-mode=response}), the counterpart to the ephemeral cache's
 * response-channel ingress.
 * <p>
 * Shared by every interface server's unclustered path (HTTP, WS, SSE, near) so the wiring lives in
 * one place. Mirrors the gateway client: a response {@link Subscription} receives responses/updates,
 * and an {@link ExclusivePublication} carrying {@code response-correlation-id} (the response
 * subscription's registration id) lets the server route responses back without an application level
 * handshake.
 */
@Log4j2
public final class ResponseChannelCacheConnector {

    /** Default cache request endpoint used when {@code EPHEMERAL_REQUEST_ENDPOINT} is unset. */
    public static final String DEFAULT_REQUEST_ENDPOINT = "localhost:8075";
    /** Default cache response control endpoint used when {@code EPHEMERAL_RESPONSE_CONTROL_ENDPOINT} is unset. */
    public static final String DEFAULT_RESPONSE_CONTROL_ENDPOINT = "localhost:8076";
    /** Default request/response stream ids (matching the ephemeral cache's defaults). */
    public static final int DEFAULT_REQUEST_STREAM_ID = 200;
    public static final int DEFAULT_RESPONSE_STREAM_ID = 201;

    private ResponseChannelCacheConnector() {
    }

    /** @return the configured cache request endpoint (env {@code EPHEMERAL_REQUEST_ENDPOINT}). */
    public static String requestEndpoint() {
        return System.getenv().getOrDefault("EPHEMERAL_REQUEST_ENDPOINT", DEFAULT_REQUEST_ENDPOINT);
    }

    /** @return the configured cache response control endpoint (env {@code EPHEMERAL_RESPONSE_CONTROL_ENDPOINT}). */
    public static String responseControlEndpoint() {
        return System.getenv().getOrDefault("EPHEMERAL_RESPONSE_CONTROL_ENDPOINT", DEFAULT_RESPONSE_CONTROL_ENDPOINT);
    }

    /** @return the configured request stream id (env {@code EPHEMERAL_REQUEST_STREAM_ID}). */
    public static int requestStreamId() {
        return envInt("EPHEMERAL_REQUEST_STREAM_ID", DEFAULT_REQUEST_STREAM_ID);
    }

    /** @return the configured response stream id (env {@code EPHEMERAL_RESPONSE_STREAM_ID}). */
    public static int responseStreamId() {
        return envInt("EPHEMERAL_RESPONSE_STREAM_ID", DEFAULT_RESPONSE_STREAM_ID);
    }

    private static int envInt(String name, int defaultValue) {
        final String value = System.getenv(name);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.warn("Couldn't parse {} as int, using default {}", name, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Connect to the ephemeral cache and start polling responses on a dedicated agent thread.
     *
     * @param aeron                    the Aeron client connected to the same media driver.
     * @param requestEndpoint          the cache's request endpoint (host:port).
     * @param requestStreamId          the cache request stream id.
     * @param responseControlEndpoint  the cache's response control endpoint (host:port).
     * @param responseStreamId         the cache response stream id.
     * @param egressFragmentHandler    handler invoked (on the poller thread) for each response frame;
     *                                 it is wrapped in a {@link FragmentAssembler} so responses larger
     *                                 than a single MTU are delivered whole.
     * @param pollIdleStrategy         idle strategy for the response poller agent.
     * @param offerIdleStrategy        idle strategy used to back off while the request publication is
     *                                 backpressured or connecting.
     * @param roleName                 the poller agent's role name.
     * @return an {@link AeronCache} whose {@code offer} publishes requests to the cache.
     */
    public static AeronCache connect(Aeron aeron,
                                     String requestEndpoint,
                                     int requestStreamId,
                                     String responseControlEndpoint,
                                     int responseStreamId,
                                     FragmentHandler egressFragmentHandler,
                                     IdleStrategy pollIdleStrategy,
                                     IdleStrategy offerIdleStrategy,
                                     String roleName) {

        final Subscription responseSubscription = aeron.addSubscription(
                new ChannelUriStringBuilder()
                        .media("udp")
                        .controlMode("response")
                        .controlEndpoint(responseControlEndpoint)
                        .build(),
                responseStreamId);

        // An ExclusivePublication is required: the response-correlation-id (the response
        // subscription's registration id) is what the server keys each client's response publication
        // on. A concurrent addPublication cannot carry it. Safe because offer is driven by a single
        // client agent thread.
        final ExclusivePublication requestPublication = aeron.addExclusivePublication(
                new ChannelUriStringBuilder()
                        .media("udp")
                        .endpoint(requestEndpoint)
                        .responseCorrelationId(responseSubscription.registrationId())
                        .build(),
                requestStreamId);

        log.info("Ephemeral response-channel client: request {} stream {}, response control {} stream {}",
                requestEndpoint, requestStreamId, responseControlEndpoint, responseStreamId);

        final AeronCache cache = new AeronCache() {
            @Override
            public void sendKeepAlive() {
            }

            @Override
            public int pollEgress() {
                return 0;
            }

            @Override
            public long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int length) {
                long res;
                while ((res = requestPublication.offer(msgBuffer, msgBufferOffset, length)) < 0) {
                    offerIdleStrategy.idle();
                }
                return res;
            }

            @Override
            public boolean isConnected() {
                // Readiness means "can reach the cache to send requests". Gate on the request
                // publication only: with Aeron response channels the response subscription stays
                // disconnected until the server sends the first response (lazy connect), which never
                // happens if we wait for it before sending - a chicken-and-egg deadlock. The request
                // publication connects on its own once the cache's request subscription is up, and the
                // response subscription then connects when the first response is routed back.
                return requestPublication.isConnected();
            }

            @Override
            public void close() {
                requestPublication.revokeOnClose();
                CloseHelper.quietCloseAll(requestPublication, responseSubscription);
            }
        };

        final FragmentAssembler egressAssembler = new FragmentAssembler(egressFragmentHandler);
        final Agent pollAgent = new Agent() {
            @Override
            public int doWork() {
                return responseSubscription.poll(egressAssembler, Integer.MAX_VALUE);
            }

            @Override
            public String roleName() {
                return roleName;
            }
        };

        final AgentRunner runner = new AgentRunner(pollIdleStrategy, Throwable::printStackTrace, null, pollAgent);
        AgentRunner.startOnThread(runner);

        return cache;
    }
}
