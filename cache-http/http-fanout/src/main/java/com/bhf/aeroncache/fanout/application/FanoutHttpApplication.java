package com.bhf.aeroncache.fanout.application;

import com.bhf.aeroncache.fanout.client.GatewayFanoutClient;
import com.bhf.aeroncache.fanout.config.FanoutConfig;
import com.bhf.aeroncache.fanout.handlers.FanoutRouteHandlers;
import com.bhf.aeroncache.fanout.metrics.FanoutMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Handler;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.extern.log4j.Log4j2;

import java.util.function.Consumer;

/**
 * HTTP fan-out aggregator for a multi-cache deployment.
 *
 * <p>Fronts several {@code http-server-javalin} gateways - each in front of its own, disjoint cache
 * cluster - and exposes the same admin/observability endpoints those gateways serve. Each request is
 * scattered to every downstream gateway over HTTP and the responses are gathered back into a single
 * result with the same JSON shape, so existing clients (the UI, scripts) can point at the aggregator
 * unchanged to see a cluster-wide view: all caches, summed stats, every node's Aeron counters, and
 * cluster-wide snapshot / purge / shutdown actions.
 *
 * <p>Unlike the HTTP gateway this is a pure HTTP aggregator: it holds no Aeron cluster connection and
 * no near cache - it only makes outbound HTTP calls to the configured gateways.
 */
@Log4j2
public class FanoutHttpApplication {

    public static final String PROMETHEUS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private static FanoutMetrics metrics;
    public static int BOUND_PORT;

    public static void main(String[] args) {
        var config = FanoutConfig.fromEnv();
        BOUND_PORT = start(config);
    }

    /**
     * Start the aggregator with the given configuration.
     *
     * @param config the runtime configuration.
     * @return the bound HTTP port.
     */
    public static int start(FanoutConfig config) {
        log.info("Starting HTTP fan-out aggregator over {} gateway(s): {}",
                config.gateways().size(), config.gateways());
        if (config.gateways().isEmpty()) {
            log.warn("No GATEWAY_ADDRESSES configured - fan-out responses will be empty until gateways are set.");
        }

        var mapper = new ObjectMapper();
        var client = new GatewayFanoutClient(config.gateways(), config.requestTimeout(), mapper);
        var handlers = new FanoutRouteHandlers(client);

        var app = startHTTPServer(config);
        wireRoutes(app, handlers);

        int port = app.port();
        log.info("Started HTTP fan-out aggregator on port {}", port);
        return port;
    }

    private static Javalin startHTTPServer(FanoutConfig config) {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "aeron-cache-http-fanout");
        metrics = new FanoutMetrics(registry);

        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);

        MicrometerPlugin micrometerPlugin =
                new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);

        return Javalin.create(getHTTPConfig(config, micrometerPlugin))
                .get("/prometheus", ctx -> ctx.contentType(PROMETHEUS_CONTENT_TYPE).result(registry.scrape()))
                .get("/liveness/", FanoutHttpApplication::handleLiveness)
                .get("/readiness/", FanoutHttpApplication::handleReadiness)
                .start(config.port());
    }

    private static void wireRoutes(Javalin app, FanoutRouteHandlers handlers) {
        app.get("/api/v1/caches", counted("getCaches", handlers::handleGetCaches))
                .get("/api/v1/counters-caches", counted("getCounterCaches", handlers::handleGetCounterCaches))
                .get("/api/v1/stats", counted("getStats", handlers::handleGetStats))
                .get("/api/v1/counters-stats", counted("getCounterStats", handlers::handleGetCounterStats))
                .get("/api/v1/timers", counted("getTimers", handlers::handleGetTimers))
                .get("/api/v1/snapshot-info", counted("getSnapshotInfo", handlers::handleGetSnapshotInfo))
                .get("/api/v1/multi/snapshot-info",
                        counted("getSnapshotInfoMulti", handlers::handleGetSnapshotInfoMulti))
                .get("/api/v1/aeron-counters", counted("getAeronCounters", handlers::handleGetAeronCounters))
                .post("/api/v1/snapshot", counted("takeSnapshot", handlers::handleSnapshot))
                .post("/api/v1/multi/snapshot", counted("takeSnapshotMulti", handlers::handleSnapshotMulti))
                .post("/api/v1/snapshot-and-purge", counted("snapshotAndPurge", handlers::handleSnapshotAndPurge))
                .post("/api/v1/shutdown", counted("shutdownCluster", handlers::handleShutdown));
    }

    /**
     * Wrap a handler so each invocation records one {@code aeroncache_fanout_operations_total} count
     * tagged with the given operation.
     *
     * @param operation the operation name.
     * @param delegate  the handler to invoke.
     * @return the wrapped handler.
     */
    private static Handler counted(String operation, Handler delegate) {
        return ctx -> {
            metrics.recordOperation(operation);
            delegate.handle(ctx);
        };
    }

    private static Consumer<JavalinConfig> getHTTPConfig(FanoutConfig config, MicrometerPlugin micrometerPlugin) {
        return cfg -> {
            cfg.showJavalinBanner = false;
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                for (var origin : config.corsOrigins()) {
                    rule.allowHost(origin);
                }
            }));
            cfg.registerPlugin(micrometerPlugin);
        };
    }

    /** The aggregator is live as soon as its HTTP interface is up; it holds no cluster connection. */
    private static void handleLiveness(io.javalin.http.Context ctx) {
        ctx.status(200).result("Live");
    }

    /** The aggregator is ready as soon as its HTTP interface is up; downstream gateways are per-request. */
    private static void handleReadiness(io.javalin.http.Context ctx) {
        ctx.status(200).result("Ready");
    }
}
