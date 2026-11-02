package com.bhf.aeroncache.metrics;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer-backed metrics shared by the web edge servers (HTTP, WebSocket and SSE). These are
 * registered on each server's {@link MeterRegistry} and exposed on its {@code /prometheus} endpoint,
 * so the web gateways get first-class observability without relying on Aeron media driver counters.
 * <p>
 * This covers the two metric families common to the web gateways:
 * <ul>
 *     <li>{@value #OPERATIONS_TOTAL} - a counter per client operation, tagged {@code operation}.</li>
 *     <li>the subscription gauges ({@value #SUBSCRIPTIONS_ACTIVE} and {@value #SUBSCRIBERS_ACTIVE}),
 *     registered by the subscription publisher that owns the live subscription state.</li>
 * </ul>
 * The owning server distinguishes itself through the registry's common {@code application} tag (e.g.
 * {@code aeron-cache-ws}), so these metric names carry no server tag of their own.
 */
public final class WebGatewayMetrics {

    /** Counter: client operations serviced, tagged by {@code operation}. */
    public static final String OPERATIONS_TOTAL = "aeroncache.operations.total";

    /**
     * Gauge: active subscriptions (one client session registered against one cache), tagged by
     * {@code domain} (cache / counters) and {@code type} (whole / keyed).
     */
    public static final String SUBSCRIPTIONS_ACTIVE = "aeroncache.subscriptions.active";

    /** Gauge: distinct client sessions holding at least one subscription, tagged by {@code domain}. */
    public static final String SUBSCRIBERS_ACTIVE = "aeroncache.subscribers.active";

    private final MeterRegistry registry;

    public WebGatewayMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * The registry these metrics are registered on, for wiring subscription gauges.
     *
     * @return the meter registry.
     */
    public MeterRegistry registry() {
        return registry;
    }

    /**
     * Count one serviced operation.
     *
     * @param operation the operation name (e.g. {@code putItem}, {@code subscribeCache}).
     */
    public void recordOperation(String operation) {
        registry.counter(OPERATIONS_TOTAL, "operation", operation).increment();
    }
}
