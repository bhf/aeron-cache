package com.bhf.aeroncache.application.ephemeral;

import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

/**
 * The client facing Aeron endpoint for the ephemeral (unclustered) cache.
 * <p>
 * Modeled on the gateway ingress agent: a single request subscription receives frames from all
 * clients, and Aeron response channels ({@code control-mode=response}) publish responses/updates
 * back to each client without an application level handshake. Session identity is the request
 * {@link Image} {@code correlationId()}; on image unavailability the session is torn down (which
 * unwinds its cache subscriptions via {@link SBEDecodingCacheClusterService#onSessionClose}).
 */
@Log4j2
public class EphemeralCacheServiceAgent implements Agent {

    private static final int FRAGMENT_LIMIT = 10;

    private final Aeron aeron;
    private final SBEDecodingCacheClusterService service;
    private final EphemeralTimerService timerService;
    private final int requestStreamId;
    private final ChannelUriStringBuilder requestUriBuilder;
    private final EphemeralSessionRegistry registry;

    private final ManyToOneConcurrentArrayQueue<Image> unavailableImages = new ManyToOneConcurrentArrayQueue<>(1024);
    private final FragmentAssembler fragmentAssembler = new FragmentAssembler(this::onFragment);

    private Subscription subscription;
    private long msgCount = 0;

    public EphemeralCacheServiceAgent(Aeron aeron,
                                      SBEDecodingCacheClusterService service,
                                      EphemeralTimerService timerService,
                                      String requestEndpoint,
                                      String responseControlEndpoint,
                                      int requestStreamId,
                                      int responseStreamId,
                                      EphemeralSessionRegistry registry) {
        this.aeron = aeron;
        this.service = service;
        this.timerService = timerService;
        this.requestStreamId = requestStreamId;
        this.registry = registry;
        this.requestUriBuilder = new ChannelUriStringBuilder()
                .media("udp")
                .endpoint(requestEndpoint)
                .responseEndpoint(responseControlEndpoint);
    }

    @Override
    public void onStart() {
        log.info("Starting unclustered Aeron Cache (response channels)");
    }

    @Override
    public int doWork() {
        int work = 0;

        if (subscription == null) {
            subscription = aeron.addSubscription(
                    requestUriBuilder.build(),
                    requestStreamId,
                    this::onImageAvailable,
                    this::onImageUnavailable);
            work++;
        }

        work += timerService.poll(System.currentTimeMillis());

        Image image;
        while ((image = unavailableImages.poll()) != null) {
            teardownSession(image);
            work++;
        }

        work += subscription.poll(fragmentAssembler, FRAGMENT_LIMIT);
        return work;
    }

    private void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final Image image = (Image) header.context();
        final ClientSession session = registry.getOrCreate(image);
        service.onSessionMessage(session, msgCount++, buffer, offset, length, header);
    }

    private void onImageAvailable(Image image) {
        log.info("Ephemeral client image available, session {}", image.correlationId());
    }

    private void onImageUnavailable(Image image) {
        if (!unavailableImages.offer(image)) {
            log.error("Unable to enqueue unavailable image for session {}", image.correlationId());
        }
    }

    private void teardownSession(Image image) {
        final long correlationId = image.correlationId();
        final ClientSession session = registry.get(correlationId);
        log.info("Tearing down ephemeral session {}", correlationId);
        if (session != null) {
            // Unwind this client's cache subscriptions so no stale offer is attempted after it leaves.
            service.onSessionClose(session, System.currentTimeMillis(), CloseReason.CLIENT_ACTION);
        }
        registry.remove(correlationId);
    }

    @Override
    public void onClose() {
        CloseHelper.quietClose(subscription);
        registry.close();
    }

    @Override
    public String roleName() {
        return "Unclustered-AC-Agent";
    }
}
