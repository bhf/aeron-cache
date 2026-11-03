package com.bhf.aeroncache.metrics;

import com.bhf.aeroncache.near.NearCacheManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer-backed metrics shared by the web edge servers (HTTP, WebSocket and SSE). These are
 * registered on each server's {@link MeterRegistry} and exposed on its {@code /prometheus} endpoint,
 * so the web gateways get first-class observability without relying on Aeron media driver counters.
 * <p>
 * This covers the metric families common to the web gateways:
 * <ul>
 *     <li>{@value #OPERATIONS_TOTAL} - a counter per client operation, tagged {@code operation}.</li>
 *     <li>the subscription gauges ({@value #SUBSCRIPTIONS_ACTIVE} and {@value #SUBSCRIBERS_ACTIVE}),
 *     registered by the subscription publisher that owns the live subscription state.</li>
 *     <li>the near cache gauges ({@value #NEAR_WARM_CACHES} and {@value #NEAR_SUBSCRIBERS}), registered
 *     only when the near cache is enabled - see {@link #registerNearWarmCaches} and the subscription
 *     publisher's near-cache gauge registration.</li>
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

    /**
     * Gauge: caches currently kept warm in the near cache, tagged by {@code domain} (cache / counters).
     * A dedicated near-cache metric, present only when near caching is enabled.
     */
    public static final String NEAR_WARM_CACHES = "aeroncache.near.warm.caches";

    /**
     * Gauge: distinct client sessions subscribed via the near cache, tagged by {@code domain} and
     * {@code type} (whole / keyed). A dedicated near-cache metric, present only when near caching is
     * enabled; it counts distinct subscriber sessions, in contrast to the general
     * {@value #SUBSCRIPTIONS_ACTIVE} gauge which counts subscriptions.
     */
    public static final String NEAR_SUBSCRIBERS = "aeroncache.near.subscribers";

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

    /**
     * Register the {@value #NEAR_WARM_CACHES} gauge for a near cache store, sampling its warm cache count
     * at scrape time. Used by the request/response HTTP near cache, whose warmth is tracked directly in a
     * {@link NearCacheManager} rather than in a streaming subscription publisher.
     *
     * @param domain           the cache domain this near cache serves ({@code cache} or {@code counters}),
     *                         attached as the {@code domain} tag.
     * @param nearCacheManager the near cache store whose warm caches are counted.
     */
    public void registerNearWarmCaches(String domain, NearCacheManager<?> nearCacheManager) {
        Gauge.builder(NEAR_WARM_CACHES, nearCacheManager, m -> m.cacheIds().size())
                .tag("domain", domain)
                .description("Caches currently kept warm in the near cache.")
                .register(registry);
    }
}
