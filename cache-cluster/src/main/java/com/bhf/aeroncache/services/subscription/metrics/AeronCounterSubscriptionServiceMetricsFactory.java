package com.bhf.aeroncache.services.subscription.metrics;

import io.aeron.Aeron;
import lombok.extern.log4j.Log4j2;

/**
 * A {@link SubscriptionServiceMetricsFactory} that registers {@link AeronCounterSubscriptionServiceMetrics}
 * against the media driver, in the style of
 * {@link com.bhf.aeroncache.services.tracing.impl.AeronCounterTracingService}.
 * <p>
 * All metrics instances created by this factory share a single Aeron client. When constructed with an
 * Aeron directory name the client is connected lazily on first use, since the media driver may not have
 * been launched when the factory is created; the lazily created client is closed on shutdown. When
 * constructed with an existing Aeron client that client is reused and not closed by this factory.
 * <p>
 * If the client cannot be connected or the counters cannot be registered, {@link #create(String)} logs
 * the failure and falls back to {@link NoOpSubscriptionServiceMetrics} so the cache keeps serving.
 */
@Log4j2
public class AeronCounterSubscriptionServiceMetricsFactory implements SubscriptionServiceMetricsFactory {

    private final String nodeId;
    private final String aeronDirectoryName;
    private final boolean ownsAeron;
    private Aeron aeron;
    private boolean initialisationFailed;

    /**
     * Create a factory that connects to the media driver in the given directory on first use.
     *
     * @param aeronDirectoryName directory of the media driver.
     * @param nodeId             id of the node, included in the counter labels.
     */
    public AeronCounterSubscriptionServiceMetricsFactory(String aeronDirectoryName, String nodeId) {
        this.aeronDirectoryName = aeronDirectoryName;
        this.nodeId = nodeId;
        this.ownsAeron = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    /**
     * Create a factory that registers its counters with an existing Aeron client. The client is not
     * closed by this factory.
     *
     * @param aeron  client connected to the media driver.
     * @param nodeId id of the node, included in the counter labels.
     */
    public AeronCounterSubscriptionServiceMetricsFactory(Aeron aeron, String nodeId) {
        this.aeron = aeron;
        this.aeronDirectoryName = null;
        this.nodeId = nodeId;
        this.ownsAeron = false;
    }

    @Override
    public SubscriptionServiceMetrics create(String subscriptionType) {
        if (initialisationFailed) {
            return new NoOpSubscriptionServiceMetrics();
        }
        try {
            if (aeron == null) {
                aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(aeronDirectoryName));
            }
            return new AeronCounterSubscriptionServiceMetrics(aeron, nodeId, subscriptionType);
        } catch (Exception e) {
            initialisationFailed = true;
            log.error("Could not register Aeron subscription metrics counters, subscription metrics are disabled", e);
            return new NoOpSubscriptionServiceMetrics();
        }
    }

    private void close() {
        if (ownsAeron && aeron != null) {
            aeron.close();
        }
    }
}
