package com.bhf.aeroncache.clustertools.application;

import com.bhf.aeroncache.clustertools.archive.RecordingPurger;
import com.bhf.aeroncache.clustertools.config.ClusterToolsConfig;
import com.bhf.aeroncache.clustertools.process.ClusterToolProcessRunner;
import com.bhf.aeroncache.clustertools.schedule.SnapshotScheduler;
import com.bhf.aeroncache.clustertools.snapshot.SnapshotInfoReader;
import com.bhf.aeroncache.http.requests.ClusterToolsRequest;
import com.bhf.aeroncache.http.responses.ClusterToolsResponse;
import com.bhf.aeroncache.http.responses.RecordingPurgeResponse;
import com.bhf.aeroncache.http.responses.SnapshotInfoResponse;
import com.bhf.aeroncache.utils.CorsUtils;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.function.Consumer;

@Log4j2
public class ClusterToolsHTTPApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    @Setter
    private static int PORT = 7080;
    private static final String API_PREFIX = "/api/v1/clustertools/";
    private static final String SNAPSHOT_INFO = API_PREFIX + "snapshot-info";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    public static int BOUND_PORT;

    private static final ClusterToolsConfig CONFIG = ClusterToolsConfig.fromEnvironment();
    private static final SnapshotInfoReader SNAPSHOT_INFO_READER = new SnapshotInfoReader();
    private static final ClusterToolProcessRunner TOOL_RUNNER = new ClusterToolProcessRunner();
    private static final RecordingPurger RECORDING_PURGER = new RecordingPurger(CONFIG);
    private static final SnapshotScheduler SNAPSHOT_SCHEDULER = new SnapshotScheduler(
            CONFIG, TOOL_RUNNER, SNAPSHOT_INFO_READER, RECORDING_PURGER,
            result -> lastPurgeResult = result);

    /**
     * Result of the most recent purge run (scheduled or on-demand), surfaced via snapshot-info.
     */
    private static volatile RecordingPurgeResponse lastPurgeResult;

    public static void main(String[] args) {
        var app = startHTTPServer();
        BOUND_PORT = app.port();
        SNAPSHOT_SCHEDULER.start();
    }

    /**
     * Start up a HTTP server for REST requests triggering ClusterTool calls.
     *
     * @return The wired up Javalin instance.
     */
    private static Javalin startHTTPServer() {

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "aeron-cache-http-cluster-tools");

        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new DiskSpaceMetrics(new File(System.getProperty("user.dir"))).bindTo(registry);

        MicrometerPlugin micrometerPlugin =
                new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);
        var config = getHTTPConfig(micrometerPlugin);

        return Javalin.create(config)
                .post(API_PREFIX, ClusterToolsHTTPApplication::handleClusterToolsRequest)
                .get(SNAPSHOT_INFO, ClusterToolsHTTPApplication::handleGetSnapshotInfo)
                .get(LIVENESS, ClusterToolsHTTPApplication::handleGetLiveness)
                .get(READINESS, ClusterToolsHTTPApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .start(PORT);
    }

    private static void handleClusterToolsRequest(Context ctx) {
        var request = ctx.bodyAsClass(ClusterToolsRequest.class);
        log.info("Got cluster tools request: {}", request);

        switch (request.tool()) {
            case "snapshot" -> runClusterTool(request, ctx, "snapshot");
            case "shutdown" -> runClusterTool(request, ctx, "shutdown");
            case "purge" -> handlePurge(ctx);
            case "snapshot-and-purge" -> handleSnapshotAndPurge(ctx);
            default -> ctx.status(400).result("Unknown tool: " + request.tool());
        }
    }

    /**
     * Purge old cluster log recording segments, reclaiming disk, without taking a new snapshot.
     *
     * @param ctx The Javalin context.
     */
    private static void handlePurge(Context ctx) {
        var response = RECORDING_PURGER.purge();
        lastPurgeResult = response;
        ctx.status(response.success() ? 200 : 500);
        ctx.json(response);
    }

    /**
     * Take a snapshot, wait for it to become durable, then purge old log segments.
     *
     * @param ctx The Javalin context.
     */
    private static void handleSnapshotAndPurge(Context ctx) {
        var response = SNAPSHOT_SCHEDULER.snapshotAndPurge();
        ctx.status(response.success() ? 200 : 500);
        ctx.json(response);
    }

    /**
     * Serve information about the latest cluster snapshot and archive disk usage for the UI.
     *
     * @param ctx The Javalin context.
     */
    private static void handleGetSnapshotInfo(Context ctx) {
        var clusterFolder = CONFIG.getClusterFolder();
        var clusterDir = clusterFolder == null ? null : new File(clusterFolder);
        SnapshotInfoResponse response = SNAPSHOT_INFO_READER.read(clusterDir, CONFIG.getArchiveDir(), lastPurgeResult);
        ctx.status(200);
        ctx.json(response);
    }

    /**
     * Run the ClusterTool as a seperate process.
     *
     * @param request The request to run ClusterTools.
     * @param ctx     The Javalin context.
     * @param command The tool command to run.
     */
    private static void runClusterTool(ClusterToolsRequest request, Context ctx, String command) {
        try {
            String requestFolder = request.clusterFolder();

            String cacheDataDir = System.getenv("CLUSTER_FOLDER");
            if (cacheDataDir != null && !cacheDataDir.isBlank()) {
                log.info("Using cache data dir: {}", cacheDataDir);
                requestFolder = cacheDataDir;
            }

            int exitCode = TOOL_RUNNER.run(requestFolder, command);

            var clusterToolsResponse = new ClusterToolsResponse(request.tool(), requestFolder, exitCode);
            ctx.status(200);
            ctx.json(clusterToolsResponse);
        } catch (Exception e) {
            log.error("Failed to execute Aeron Cache cluster tool command: {}", command, e);
            ctx.status(500).result("Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Basic configuration for CORS.
     *
     * @return Config for Javalin.
     */
    private static Consumer<JavalinConfig> getHTTPConfig(MicrometerPlugin micrometerPlugin) {
        return config -> {
            config.showJavalinBanner = false;
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    CorsUtils.getAllowedOrigins("http://localhost:3000",
                            "http://localhost:3001",
                            "http://localhost:3002",
                            "http://localhost:3003",
                            "http://localhost:3004",
                            "http://localhost:3005", "http://localhost").forEach(it::allowHost);
                });
            });

            config.registerPlugin(micrometerPlugin);
        };
    }

    /**
     * Is the application ready to process requests.
     *
     * @param ctx The context.
     */
    private static void handleGetReadiness(Context ctx) {
        ctx.status(HTTPStatusUtils.SERVICE_READY);
        ctx.result("Ready");
    }

    /**
     * Is the application live and running.
     *
     * @param ctx The context.
     */
    private static void handleGetLiveness(Context ctx) {
        ctx.status(HTTPStatusUtils.SERVICE_LIVE);
        ctx.result("Connected");
    }
}