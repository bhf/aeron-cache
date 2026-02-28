package com.bhf.aeroncache.clustertools.application;

import com.bhf.aeroncache.clustertools.model.ClusterToolsRequest;
import com.bhf.aeroncache.clustertools.model.ClusterToolsResponse;
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
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Log4j2
public class ClusterToolsHTTPApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final int PORT = 7080;
    private static final String API_PREFIX = "/api/v1/clustertools/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";

    public static void main(String[] args) {
        startHTTPServer();
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
                .get(LIVENESS, ClusterToolsHTTPApplication::handleGetLiveness)
                .get(READINESS, ClusterToolsHTTPApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .start(PORT);
    }

    private static void handleClusterToolsRequest(Context ctx) {
        var request = ctx.bodyAsClass(ClusterToolsRequest.class);
        log.info("Got cluster tools request: {}", request);

        if (request.tool().equals("snapshot")) {
            runClusterTool(request, ctx, "snapshot");
        } else if (request.tool().equals("shutdown")) {
            runClusterTool(request, ctx, "shutdown");
        }
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
            var javaHome = System.getProperty("java.home");
            var javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            var classpath = System.getProperty("java.class.path");

            List<String> args = new ArrayList<>();
            args.add(javaBin);
            args.add("--add-opens");
            args.add("java.base/jdk.internal.misc=ALL-UNNAMED");
            System.getProperties().forEach((key, value) -> args.add("-D" + key + "=" + value));

            args.add("-cp");
            args.add(classpath);
            args.add("io.aeron.cluster.ClusterTool");
            args.add(request.clusterFolder());
            args.add(command);

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.inheritIO();
            var process = pb.start();
            int exitCode = process.waitFor();
            log.info("Aeron Cache cluster tools command {} request exited with code: {}", command, exitCode);

            var clusterToolsResponse = new ClusterToolsResponse(request.tool(), request.clusterFolder(), exitCode);
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
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowHost("http://localhost:3000", "http://localhost");
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