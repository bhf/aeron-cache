package com.bhf.aeroncache.ws.application;

import com.bhf.aeroncache.services.cluster.AeronCacheListener;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.AgentRequestPublisher;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import com.bhf.aeroncache.utils.RingBufferUtils;
import com.bhf.aeroncache.ws.services.subscriptions.CacheSubscriptionService;
import io.aeron.cluster.client.AeronCluster;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.micrometer.MicrometerPlugin;
import io.javalin.websocket.*;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.trace.Span;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Log4j2
public class WebsocketApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final int PORT = 7071;
    private static final String API_PREFIX = "/api/ws/v1/cache/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static final String MULTI_SUB_API_PREFIX = "/api/ws/v1/caches/";
    private static AeronCacheListener client;
    private static CacheSubscriptionService subscriptionService;
    private static AeronCluster cluster;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    private static final CacheStatsTracker statsTracker = new CacheStatsTracker();

    private static String tracingServiceName;

    public static void main(String[] args) {
        System.out.println("Starting Websocket interface");
        tracingServiceName = System.getenv("OTEL_SERVICE_NAME");

        var app = startHTTPServer();

        try {
            ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);
            System.out.println("Starting AeronCache Cluster Interface");
            subscriptionService = new CacheSubscriptionService(new AgentRequestPublisher(rb));
            client = new AeronCacheListener();
            client.setCacheResultsCallbacks(subscriptionService);

            var allHosts = System.getenv("CLUSTER_ADDRESSES");
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);

            var egressIP = DNSUtils.getThisHostName();
            var hostArray = List.of(allHosts.split(","));
            var ingressEndpoints = ClusterUtils.ingressEndpoints(hostArray);

            System.out.println("Awaiting DNS Resolution");
            for (int i = 0; i < hostArray.size(); i++) {
                DNSUtils.awaitDnsResolution(hostArray, i);
            }

            System.out.println("DNS Resolution Complete. Building cluster connection now.");
            cluster = ClusterUtils.buildClusterConnection(egressIP, ingressEndpoints, client);

            System.out.println("Building cluster agent");
            var idleStrategy = new BackoffIdleStrategy();
            ClusterClientAgent agent = new ClusterClientAgent(cluster, rb, idleStrategy);
            var errorHandler = ClusterUtils.getAgentRunnerErrorHandler(cluster);
            var errorCounter = ClusterUtils.getAgentErrorCounter(cluster);
            AgentRunner runner = new AgentRunner(new YieldingIdleStrategy(), errorHandler, errorCounter, agent);
            clusterConnected.set(true);
            AgentRunner.startOnThread(runner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start up a HTTP server for REST requests.
     *
     * @return The wired up Javalin instance.
     */
    private static Javalin startHTTPServer() {

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "aeron-cache-ws");

        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new DiskSpaceMetrics(new File(System.getProperty("user.dir"))).bindTo(registry);

        MicrometerPlugin micrometerPlugin = new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);
        var config = getHTTPConfig(micrometerPlugin);

        return Javalin.create(config)
                .before(API_PREFIX + "*", _ -> statsTracker.getTotalOpsCount().incrementAndGet())
                .ws(API_PREFIX + "/{cacheId}", WebsocketApplication::handleSingleCacheWs)
                .ws(MULTI_SUB_API_PREFIX + "/{cacheIds}", WebsocketApplication::handleMultiCacheWs)
                .get(LIVENESS, WebsocketApplication::handleGetLiveness)
                .get(READINESS, WebsocketApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .start(PORT);
    }

    /**
     * Setup websocket for subscriptions to a single cache.
     * @param wsConfig
     */
    private static void handleSingleCacheWs(WsConfig wsConfig) {
        wsConfig.onConnect(WebsocketApplication::onSingleCacheConnect);
        wsConfig.onClose(WebsocketApplication::onWsClose);
        wsConfig.onError(WebsocketApplication::onWsError);
        wsConfig.onMessage(WebsocketApplication::onWsMessage);
    }

    /**
     * Setup websocket for subscriptions to multiple caches.
     * @param wsConfig
     */
    private static void handleMultiCacheWs(WsConfig wsConfig) {
        wsConfig.onConnect(WebsocketApplication::onMultiCacheConnect);
        wsConfig.onClose(WebsocketApplication::onWsClose);
        wsConfig.onError(WebsocketApplication::onWsError);
        wsConfig.onMessage(WebsocketApplication::onWsMessage);
    }

    private static void onWsMessage(WsMessageContext wsMessageContext) {
        log.warn("Received message from websocket sessionId: {}, message: {}", wsMessageContext.sessionId(), wsMessageContext.message());
    }

    private static void onWsError(WsErrorContext wsErrorContext) {
        log.warn("Got websocket error: {}", wsErrorContext);
        subscriptionService.handleWsError(cluster, getRequestId(wsErrorContext.getUpgradeCtx$javalin()), wsErrorContext.sessionId());
    }

    private static void onWsClose(WsCloseContext wsCloseContext) {
        log.info("Websocket closed for sessionId: {}", wsCloseContext.sessionId());
        subscriptionService.handleWsClosed(cluster, getRequestId(wsCloseContext.getUpgradeCtx$javalin()), wsCloseContext.sessionId());
    }

    /**
     * Add subscription to a single cache to the
     * websocket.
     *
     * @param wsConnectContext
     */
    private static void onSingleCacheConnect(WsConnectContext wsConnectContext) {
        try {
            wsConnectContext.enableAutomaticPings();
            var cacheId = Long.parseLong(wsConnectContext.pathParam("cacheId"));
            var requestId = getRequestId(wsConnectContext.getUpgradeCtx$javalin());
            log.info("Subscription request for cacheId: {} on ws sessionId: {}", cacheId, wsConnectContext.sessionId());
            subscriptionService.subscribeToCache(cluster, wsConnectContext, cacheId, wsConnectContext.sessionId(), requestId, wsConnectContext::send);
        } catch (NumberFormatException e) {
            statsTracker.getTotalErrors().incrementAndGet();
            log.warn("Couldn't parse cacheId correctly, path params: {}", wsConnectContext.pathParamMap());
            wsConnectContext.closeSession(WsCloseStatus.PROTOCOL_ERROR, "Couldn't parse cacheId");
        }
    }

    /**
     * Add subscriptions to multiple caches on the same
     * websocket.
     *
     * @param wsConnectContext
     */
    private static void onMultiCacheConnect(WsConnectContext wsConnectContext) {
        try {
            wsConnectContext.enableAutomaticPings();
            var cacheIds = wsConnectContext.pathParam("cacheIds");
            String[] caches = cacheIds.split(",");
            for (var c : caches) {
                var cacheId = Long.parseLong(c);
                var requestId = getRequestId(wsConnectContext.getUpgradeCtx$javalin());
                log.info("Subscription request for cacheId: {} on ws sessionId: {}", cacheId, wsConnectContext.sessionId());
                subscriptionService.subscribeToCache(cluster, wsConnectContext, cacheId, wsConnectContext.sessionId(), requestId, wsConnectContext::send);
            }
        } catch (NumberFormatException e) {
            statsTracker.getTotalErrors().incrementAndGet();
            log.warn("Couldn't parse cacheId correctly, path params: {}", wsConnectContext.pathParamMap());
            wsConnectContext.closeSession(WsCloseStatus.PROTOCOL_ERROR, "Couldn't parse cacheId");
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
        if (clusterConnected.get()) {
            ctx.status(HTTPStatusUtils.SERVICE_READY);
            ctx.result("Ready");
        } else {
            ctx.status(HTTPStatusUtils.SERVICE_NOT_READY);
            ctx.result("Cluster not connected");
        }
    }

    /**
     * Is the application live and running.
     *
     * @param ctx The context.
     */
    private static void handleGetLiveness(Context ctx) {
        if (clusterConnected.get()) {
            ctx.status(HTTPStatusUtils.SERVICE_LIVE);
            ctx.result("Connected");
        } else {
            ctx.status(HTTPStatusUtils.SERVICE_NOT_LIVE);
            ctx.result("Cluster not connected");
        }
    }

    /**
     * Build the requestId based on whether tracing is enabled.
     *
     * @param ctx The Context.
     * @return A requestId
     */
    private static String getRequestId(Context ctx) {
        return tracingServiceName != null ? getTraceBasedRequestId(ctx) : UUID.randomUUID().toString();
    }

    /**
     * Use the current span and trace Ids to build a requestId to
     * be sent to the Aeron Cache cluster.
     *
     * @param ctx
     * @return
     */
    private static String getTraceBasedRequestId(Context ctx) {
        var currentSpanId = Span.current().getSpanContext().getSpanId();
        var currentTraceId = Span.current().getSpanContext().getTraceId();
        log.info("Creating requestId using traceID {} and spanID {}", currentTraceId, currentSpanId);
        return STR."\{currentTraceId}@\{currentSpanId}";
    }

}
