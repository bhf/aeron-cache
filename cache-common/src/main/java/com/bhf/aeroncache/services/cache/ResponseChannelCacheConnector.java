package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.utils.ClusterUtils;
import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.ExclusivePublication;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

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
     * @param offerIdleStrategy        idle strategy used to back off while the request ring buffer is full.
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

        final ReconnectingResponseChannelCache cache = new ReconnectingResponseChannelCache(
                aeron, requestEndpoint, requestStreamId, responseControlEndpoint, responseStreamId,
                egressFragmentHandler, offerIdleStrategy, roleName);

        final AgentRunner runner = new AgentRunner(pollIdleStrategy, Throwable::printStackTrace, null, cache);
        AgentRunner.startOnThread(runner);

        return cache;
    }

    /**
     * An {@link AeronCache} over Aeron response channels that re-establishes its channel when the cache
     * goes away (for example a cache restart). Unlike the old fixed-endpoint transport - where responses
     * flowed to a stable interface endpoint and so survived a restart automatically - a response channel
     * is bound to the server session that created it, so when the cache restarts the response direction
     * is dead until the client opens a fresh response subscription + request publication.
     * <p>
     * Modeled on the gateway client: the poller agent (single thread) owns the whole channel lifecycle
     * and is the <em>only</em> writer of the request {@link ExclusivePublication}. Callers hand request
     * frames to the agent through a {@link ManyToOneRingBuffer} ({@link #offer} writes, the agent
     * drains), which keeps the exclusive-publication single-writer contract while letting the agent also
     * (a) rebuild the channel on a cache restart and (b) send warm-up frames so the response channel is
     * established even before the first real request. {@link #isConnected()} reports the response
     * channel being live (true end-to-end readiness), which the warm-up makes reachable without a real
     * request.
     */
    private static final class ReconnectingResponseChannelCache implements AeronCache, Agent {

        private static final int FRAGMENT_LIMIT = 10;
        private static final int COMMAND_LIMIT = 16;
        private static final int COMMAND_MSG_TYPE_ID = 1;
        /**
         * SBE template id used for warm-up frames. It sits far outside the cache's real template id
         * range (1-54), so {@code onSessionMessage} matches no handler and ignores it - the frame's only
         * purpose is to make the cache create this client's response publication (which it does for any
         * inbound frame, before decoding), establishing the response channel.
         */
        private static final int WARMUP_TEMPLATE_ID = 60000;
        private static final int WARMUP_FRAME_LENGTH = 8;
        private static final long WARMUP_INTERVAL_NS = TimeUnit.MILLISECONDS.toNanos(50);

        private final Aeron aeron;
        private final int requestStreamId;
        private final int responseStreamId;
        private final String requestEndpoint;
        private final String responseControlEndpoint;
        private final IdleStrategy offerIdleStrategy;
        private final String roleName;
        private final FragmentAssembler egressAssembler;

        private final ManyToOneRingBuffer commandRingBuffer;
        private final ControlledMessageHandler commandHandler = this::onCommand;
        private final UnsafeBuffer warmupBuffer;

        private volatile ExclusivePublication requestPublication;
        private volatile Subscription responseSubscription;
        private boolean requestEverConnected;
        private boolean responseEverConnected;
        private long lastWarmupNs;

        private ReconnectingResponseChannelCache(Aeron aeron,
                                                 String requestEndpoint,
                                                 int requestStreamId,
                                                 String responseControlEndpoint,
                                                 int responseStreamId,
                                                 FragmentHandler egressFragmentHandler,
                                                 IdleStrategy offerIdleStrategy,
                                                 String roleName) {
            this.aeron = aeron;
            this.requestEndpoint = requestEndpoint;
            this.requestStreamId = requestStreamId;
            this.responseControlEndpoint = responseControlEndpoint;
            this.responseStreamId = responseStreamId;
            this.offerIdleStrategy = offerIdleStrategy;
            this.roleName = roleName;
            this.egressAssembler = new FragmentAssembler(egressFragmentHandler);

            // Match the interface's ingress ring buffer sizing so large requests fit (max message =
            // capacity / 8). Term length is the single source of truth for message sizing here.
            final int capacity = ClusterUtils.getConfiguredTermLength(16 * 1024 * 1024);
            this.commandRingBuffer = new ManyToOneRingBuffer(new UnsafeBuffer(
                    ByteBuffer.allocateDirect(capacity + RingBufferDescriptor.TRAILER_LENGTH)));

            this.warmupBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(WARMUP_FRAME_LENGTH));
            this.warmupBuffer.putShort(2, (short) WARMUP_TEMPLATE_ID, java.nio.ByteOrder.LITTLE_ENDIAN);
        }

        // ----- lifecycle (poller thread only) -----

        private void open() {
            final Subscription sub = aeron.addSubscription(
                    new ChannelUriStringBuilder()
                            .media("udp")
                            .controlMode("response")
                            .controlEndpoint(responseControlEndpoint)
                            .build(),
                    responseStreamId);

            // An ExclusivePublication is required: the response-correlation-id (the response
            // subscription's registration id) is what the server keys each client's response
            // publication on. A concurrent addPublication cannot carry it. Safe because the agent thread
            // is the only writer.
            final ExclusivePublication pub = aeron.addExclusivePublication(
                    new ChannelUriStringBuilder()
                            .media("udp")
                            .endpoint(requestEndpoint)
                            .responseCorrelationId(sub.registrationId())
                            .build(),
                    requestStreamId);

            responseSubscription = sub;
            requestPublication = pub;
            requestEverConnected = false;
            responseEverConnected = false;
            lastWarmupNs = 0;
            log.info("Ephemeral response-channel client: request {} stream {}, response control {} stream {}",
                    requestEndpoint, requestStreamId, responseControlEndpoint, responseStreamId);
        }

        private void reopen() {
            final Subscription oldSub = responseSubscription;
            final ExclusivePublication oldPub = requestPublication;
            log.info("Cache connection lost; rebuilding response channel");
            open();
            if (oldPub != null) {
                oldPub.revokeOnClose();
            }
            CloseHelper.quietCloseAll(oldPub, oldSub);
        }

        @Override
        public int doWork() {
            if (responseSubscription == null) {
                open();
                return 1;
            }

            final ExclusivePublication pub = requestPublication;
            final Subscription sub = responseSubscription;

            if (pub.isConnected()) {
                requestEverConnected = true;
            }
            if (sub.isConnected()) {
                responseEverConnected = true;
            }

            // Detect a cache restart and rebuild the channel so a fresh response session forms with the
            // new cache. Either signal means the old cache is gone: the request publication dropped after
            // connecting, or the response subscription's image (the server's response publication) went
            // away after a response had established it.
            if ((requestEverConnected && !pub.isConnected()) || (responseEverConnected && !sub.isConnected())) {
                reopen();
                return 1;
            }

            int work = 0;
            // Once the request side is up but the response channel has not yet formed, nudge the cache to
            // create this client's response publication so the response subscription connects. This makes
            // isConnected() (response based) reachable without waiting for a real request, and re-arms it
            // after a rebuild.
            if (pub.isConnected() && !sub.isConnected()) {
                work += sendWarmup(pub);
            }

            work += commandRingBuffer.controlledRead(commandHandler, COMMAND_LIMIT);
            work += sub.poll(egressAssembler, FRAGMENT_LIMIT);
            return work;
        }

        private int sendWarmup(ExclusivePublication pub) {
            final long now = System.nanoTime();
            if (now - lastWarmupNs < WARMUP_INTERVAL_NS) {
                return 0;
            }
            lastWarmupNs = now;
            pub.offer(warmupBuffer, 0, WARMUP_FRAME_LENGTH);
            return 1;
        }

        /**
         * Drain a single request frame from the ring buffer and offer it to the request publication.
         * Runs on the agent thread only (single writer). Returns {@link ControlledMessageHandler.Action#ABORT}
         * to leave the frame in place (preserving FIFO order) when the publication is not ready or is
         * backpressured, so it is retried on the next duty cycle.
         */
        private ControlledMessageHandler.Action onCommand(int msgTypeId, MutableDirectBuffer buffer, int index, int length) {
            final ExclusivePublication pub = requestPublication;
            if (pub == null) {
                return ControlledMessageHandler.Action.ABORT;
            }
            final long result = pub.offer(buffer, index, length);
            if (result > 0) {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            if (result == Publication.CLOSED || result == Publication.MAX_POSITION_EXCEEDED) {
                log.warn("Dropping request frame of length {}; publication offer result {}", length, result);
                return ControlledMessageHandler.Action.CONTINUE;
            }
            return ControlledMessageHandler.Action.ABORT;
        }

        @Override
        public String roleName() {
            return roleName;
        }

        @Override
        public void onClose() {
            close();
        }

        // ----- AeronCache -----

        @Override
        public void sendKeepAlive() {
        }

        @Override
        public int pollEgress() {
            return 0;
        }

        @Override
        public long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int length) {
            // Hand the frame to the agent thread (the sole publication writer) through the ring buffer.
            // Returns a positive value on success (the caller's while (offer < 0) loop treats it as
            // sent) or a negative under backpressure so the caller retries.
            while (!commandRingBuffer.write(COMMAND_MSG_TYPE_ID, msgBuffer, msgBufferOffset, length)) {
                offerIdleStrategy.idle();
            }
            return length;
        }

        @Override
        public boolean isConnected() {
            // Readiness means "a request can round-trip". Gate on the response subscription being live:
            // the warm-up above establishes it shortly after the request publication connects, and it
            // drops (triggering a rebuild) when the cache goes away - so readiness reflects an actually
            // working response channel rather than a request side that may still look connected during
            // the window before a dead cache is detected.
            final Subscription sub = responseSubscription;
            return sub != null && sub.isConnected();
        }

        @Override
        public void close() {
            final ExclusivePublication pub = requestPublication;
            if (pub != null) {
                pub.revokeOnClose();
            }
            CloseHelper.quietCloseAll(pub, responseSubscription);
        }
    }
}
