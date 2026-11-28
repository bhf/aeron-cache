package com.bhf.aeroncache.fanout.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal Micrometer metrics for the fan-out aggregator: one {@code aeroncache_fanout_operations_total}
 * counter per endpoint operation, plus the JVM/process meters bound onto the shared registry by the
 * application. Kept local to this module so the pure HTTP aggregator need not depend on the heavier
 * {@code cache-client} {@code WebGatewayMetrics} (which carries the Aeron cache client plumbing).
 */
public class FanoutMetrics {

    private static final String OPERATIONS_TOTAL = "aeroncache_fanout_operations_total";

    private final PrometheusMeterRegistry registry;
    private final Map<String, Counter> operationCounters = new ConcurrentHashMap<>();

    /**
     * @param registry the shared Prometheus registry served on {@code /prometheus}.
     */
    public FanoutMetrics(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * The backing registry, so the application can bind JVM/process meters and scrape it.
     *
     * @return the registry.
     */
    public PrometheusMeterRegistry registry() {
        return registry;
    }

    /**
     * Record one invocation of the given endpoint operation.
     *
     * @param operation the operation name (used as the {@code operation} tag).
     */
    public void recordOperation(String operation) {
        operationCounters.computeIfAbsent(operation,
                op -> Counter.builder(OPERATIONS_TOTAL).tag("operation", op).register(registry)).increment();
    }
}
