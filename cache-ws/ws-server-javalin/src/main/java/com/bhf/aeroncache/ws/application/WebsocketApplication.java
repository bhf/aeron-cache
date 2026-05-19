package com.bhf.aeroncache.ws.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.RequestErrorResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cacheclient.CacheClientFactory;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.services.cluster.impl.RBClusterMessagePublisher;
import com.bhf.aeroncache.utils.*;
import com.bhf.aeroncache.ws.config.WsIdleStrategies;
import io.aeron.Aeron;
import io.aeron.RethrowingErrorHandler;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.servlet.JavalinServletContext;
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
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Log4j2
public class WebsocketApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    @Setter
    private static int DEFAULT_WS_PORT = 7071;
    private static final String API_PREFIX = "/api/ws/v1/cache/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static final String MULTI_SUB_API_PREFIX = "/api/ws/v1/caches/";
    private static final boolean PRE_ENCODE_CACHE_REQUESTS = false;
    private static AeronCacheClusterListener client;
    private static CacheSubscriptionRequestPublisher subscriptionService;
    private static AeronCache cache;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    private static final CacheStatsTracker statsTracker = new CacheStatsTracker();

    private static String tracingServiceName;

    private static AgentRunner agentRunner;
    private static AeronCluster aeronCluster;
    private static MediaDriver mediaDriver;
    private static boolean CLUSTERED_MODE;
    public static int BOUND_PORT;

    public static void main(String[] args) {
        BOUND_PORT = startWebsocketInterface(DEFAULT_WS_PORT);
    }

    public static int startWebsocketInterface(int port) {
        System.out.println("Starting Websocket interface");
        tracingServiceName = System.getenv("OTEL_SERVICE_NAME");

        var app = startHTTPServer(port);

        try {
            ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);
            System.out.println("Starting AeronCache Cluster Interface");

            CacheClientFactory clientFactory = getCacheClientFactory();
            var cacheRequestEncoder = clientFactory.getCacheRequestEncoder();
            var responseDecoder = clientFactory.getCacheResponseDecoder();
            var schemaDetailsProvider = clientFactory.getSchemaDetails();
            var indexSupplier = clientFactory.getIndexSupplier();
            var keySupplier = clientFactory.getKeySupplier();
            var valueSupplier = clientFactory.getValueSupplier();

            if (PRE_ENCODE_CACHE_REQUESTS) {
                // We encode the SBE messages before dropping them onto an Agrona RB for
                // sending directly to the cluster
                var requestPublisher = new RBClusterMessagePublisher(cache, rb,
                        WsIdleStrategies.clusterMessagePublisherIdleStrategy.get(), cacheRequestEncoder);
                subscriptionService = new CacheSubscriptionRequestPublisher(requestPublisher);
            } else {
                // Drop normalised cache requests onto an Agrona RB for encoding
                // to SBE on the Agent thread
                CacheRequestPublisher rbPublisher = new RBCacheRequestPublisher(rb);
                subscriptionService = new CacheSubscriptionRequestPublisher(rbPublisher);
            }

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider, indexSupplier, keySupplier, valueSupplier);
            client.setCacheResultsCallbacks(subscriptionService);

            var allHosts = System.getenv("CLUSTER_ADDRESSES");
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);

            var egressIP = DNSUtils.getThisHostName();
            var hostArray = allHosts!=null ? List.of(allHosts.split(",")) : List.of("localhost");
            var ingressEndpoints = ClusterUtils.ingressEndpoints(hostArray);

            System.out.println("Awaiting DNS Resolution");
            for (int i = 0; i < hostArray.size(); i++) {
                DNSUtils.awaitDnsResolution(hostArray, i);
            }

            System.out.println("DNS Resolution Complete. Building cluster connection now.");
            var launchEmbeddedStr = System.getenv("LAUNCH_EMBEDDED");
            boolean launchEmbedded = launchEmbeddedStr == null || Boolean.parseBoolean(launchEmbeddedStr);
            if (launchEmbedded) {
                mediaDriver = ClusterUtils.launchEmbeddedMediaDriver();
            }

            final Aeron.Context aeronCtx = new Aeron.Context();
            if (launchEmbedded) {
                aeronCtx.aeronDirectoryName(mediaDriver.aeronDirectoryName());
            } else {
                String externalAeronDir = System.getenv("AERON_DIR");
                if (externalAeronDir != null) {
                    aeronCtx.aeronDirectoryName(externalAeronDir);
                }
            }

            var cacheMode = System.getenv("CACHE_MODE");
            CLUSTERED_MODE = cacheMode==null || cacheMode.toUpperCase().equals("RAFT");

            System.out.println("Cache mode: "+cacheMode+", using clustered mode: "+CLUSTERED_MODE);

            if (CLUSTERED_MODE) {
                buildClusterConnection(egressIP, ingressEndpoints, aeronCtx.aeronDirectoryName());
            } else {
                final Aeron aeron = Aeron.connect(aeronCtx);
                var requestPubHost = System.getenv("REQUEST_PUB_HOST");
                buildUnclusteredConnection(aeron, requestPubHost);
            }

            System.out.println("Building cluster agent for websocket service");
            var clusterClientAgentIdleStrategy = CLUSTERED_MODE ? WsIdleStrategies.clusterClientAgentIdleStrategy.get() : WsIdleStrategies.unclusteredIdleStrategy.get();
            var clusterMessagePublisherIdleStrategy = CLUSTERED_MODE ? WsIdleStrategies.clusterMessagePublisherIdleStrategy.get() : WsIdleStrategies.unclusteredIdleStrategy.get();
            var agentRunnerIdleStrategy = CLUSTERED_MODE ? WsIdleStrategies.agentRunnerIdleStrategy.get() : WsIdleStrategies.unclusteredIdleStrategy.get();

            var agent = PRE_ENCODE_CACHE_REQUESTS ?
                    new ClusterClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-CacheClient-Agent") :
                    new CacheClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-CacheClient-Agent");

            var errorHandler = aeronCluster != null ? ClusterUtils.getAgentRunnerErrorHandler(aeronCluster) :
                    new RethrowingErrorHandler();
            var errorCounter = aeronCluster != null ? ClusterUtils.getAgentErrorCounter(aeronCluster, "WSClient") :
                    null;
            agentRunner = new AgentRunner(agentRunnerIdleStrategy, errorHandler, errorCounter, agent);
            clusterConnected.set(true);
            AgentRunner.startOnThread(agentRunner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("Started websocket on port {}", app.port());
        return app.port();
    }

    private static CacheClientFactory getCacheClientFactory() {
        ServiceLoader<CacheClientFactory> service = ServiceLoader.load(CacheClientFactory.class);
        Optional<CacheClientFactory> first = service.findFirst();

        if (first.isPresent()) {
            return first.get();
        } else {
            throw new IllegalStateException("No CacheClientFactory found.");
        }
    }

    private static void buildUnclusteredConnection(Aeron aeron, String requestPubHost) {

        var requestPublicationChannel = "aeron:udp?endpoint="+requestPubHost+":7008|alias=AC-unclustered-requests";
        int requestPublicationStream = 1;
        var requestPublication = aeron.addPublication(requestPublicationChannel,
                requestPublicationStream);

        var hostname = DNSUtils.getThisHostName();
        String responseSubscriptionChannel = "aeron:udp?endpoint="+hostname+":7007|alias=AC-unclustered-responses";
        int responseSubscriptionStream = 2;
        var responseSubscription = aeron.addSubscription(responseSubscriptionChannel,
                responseSubscriptionStream);

        cache = new AeronCache() {
            @Override
            public void sendKeepAlive() {
            }

            @Override
            public int pollEgress() {
                return 0;
            }

            @Override
            public long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int i) {
                long res = 0;
                while ((res = requestPublication.offer(msgBuffer, msgBufferOffset, i)) < 0) {
                    aeron.context().idleStrategy().idle();
                }
                return res;
            }

            @Override
            public boolean isConnected() {
                return true;
            }
        };

        AeronCacheClusterListener egressListener = client;
        FragmentHandler egressFragmentHandler = (buffer, offset, length, header)
                -> egressListener.onMessage(header.sessionId(),
                System.currentTimeMillis(),
                buffer, offset, length, header);

        Agent serverAgent = new Agent() {
            @Override
            public int doWork() throws Exception {
                return responseSubscription.poll(egressFragmentHandler, 10);
            }

            @Override
            public String roleName() {
                return "AC-Unclustered-requests-listener";
            }
        };

        IdleStrategy unclusteredAgentIdleStrategy = WsIdleStrategies.unclusteredIdleStrategy.get();
        final AgentRunner serverAgentRunner = new AgentRunner(unclusteredAgentIdleStrategy,
                Throwable::printStackTrace,
                null, serverAgent);

        AgentRunner.startOnThread(serverAgentRunner);
    }

    private static void buildClusterConnection(String egressIP, String ingressEndpoints, String aeronDirectory) {
        try {
            aeronCluster = ClusterUtils.buildClusterConnection(egressIP, ingressEndpoints, client, "WSClient",
                    aeronDirectory);
            addClusterErrorHandler(aeronCluster);

            cache = new AeronCache() {
                @Override
                public void sendKeepAlive() {
                    aeronCluster.sendKeepAlive();
                }

                @Override
                public int pollEgress() {
                    return aeronCluster.pollEgress();
                }

                @Override
                public long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int i) {
                    return aeronCluster.offer(msgBuffer, msgBufferOffset, i);
                }

                @Override
                public boolean isConnected() {
                    return !aeronCluster.isClosed();
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Will try to reconnect");
            buildClusterConnection(egressIP, ingressEndpoints, aeronDirectory);
        }
    }

    public static void shutdown() {
        CloseHelper.close(aeronCluster);
        CloseHelper.close(mediaDriver);
        CloseHelper.close(agentRunner);
    }

    private static void addClusterErrorHandler(AeronCluster aeronCluster) {
        aeronCluster.context().errorHandler(throwable -> clusterConnected.set(false));
    }

    /**
     * Start up a HTTP server for REST requests.
     *
     * @return The wired up Javalin instance.
     */
    private static Javalin startHTTPServer(int port) {

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "aeron-cache-ws");

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
                .beforeMatched(WebsocketApplication::checkClusterConnectivity)
                .before(API_PREFIX + "*", _ -> statsTracker.getTotalOpsCount().incrementAndGet())
                .ws(API_PREFIX + "/{cacheId}", WebsocketApplication::handleSingleCacheWs)
                .ws(MULTI_SUB_API_PREFIX + "/{cacheIds}", WebsocketApplication::handleMultiCacheWs)
                .get(LIVENESS, WebsocketApplication::handleGetLiveness)
                .get(READINESS, WebsocketApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .start(port);
    }

    private static void checkClusterConnectivity(Context ctx) {
        if (cache == null || !cache.isConnected()) {
            log.warn("Cluster not connected");
            ctx.status(HTTPStatusUtils.SERVICE_NOT_LIVE);
            var errorResponse = new RequestErrorResponse("Cluster not connected", ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.json(errorResponse);
            ((JavalinServletContext) ctx).getTasks().clear();
        }
    }

    /**
     * Setup websocket for subscriptions to a single cache.
     *
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
     *
     * @param wsConfig
     */
    private static void handleMultiCacheWs(WsConfig wsConfig) {
        wsConfig.onConnect(WebsocketApplication::onMultiCacheConnect);
        wsConfig.onClose(WebsocketApplication::onWsClose);
        wsConfig.onError(WebsocketApplication::onWsError);
        wsConfig.onMessage(WebsocketApplication::onWsMessage);
    }

    private static void onWsMessage(WsMessageContext wsMessageContext) {
        log.warn("Received message from websocket sessionId: {}, message: {}", wsMessageContext.sessionId(),
                wsMessageContext.message());
    }

    private static void onWsError(WsErrorContext wsErrorContext) {
        log.warn("Got websocket error: {}", wsErrorContext);
        subscriptionService.handleWsError(cache, getRequestId(wsErrorContext.getUpgradeCtx$javalin()),
                wsErrorContext.sessionId());
    }

    private static void onWsClose(WsCloseContext wsCloseContext) {
        log.info("Websocket closed for sessionId: {}", wsCloseContext.sessionId());

        if(CLUSTERED_MODE) {
            subscriptionService.handleWsClosed(cache, getRequestId(wsCloseContext.getUpgradeCtx$javalin()),
                    wsCloseContext.sessionId());
        }
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
            var cacheId = wsConnectContext.pathParam("cacheId");
            var requestId = getRequestId(wsConnectContext.getUpgradeCtx$javalin());
            log.info("Subscription request for cacheId: {} on ws sessionId: {}", cacheId, wsConnectContext.sessionId());

            final Consumer<Void> subscriptionFailureHandler = _ ->
                    wsConnectContext.closeSession(WsCloseStatus.SERVER_ERROR, "Couldn't subscribe to cache");
            subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, cacheId, wsConnectContext.sessionId(),
                    requestId, wsConnectContext::send);
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
                var requestId = getRequestId(wsConnectContext.getUpgradeCtx$javalin());
                log.info("Subscription request for cacheId: {} on ws sessionId: {}", c,
                        wsConnectContext.sessionId());
                final Consumer<Void> subscriptionFailureHandler = _ ->
                        wsConnectContext.closeSession(WsCloseStatus.SERVER_ERROR, "Couldn't subscribe to cache");

                subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, c, wsConnectContext.sessionId(),
                        requestId, wsConnectContext::send);
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
            config.showJavalinBanner = false;
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowHost("http://localhost:3000",
                            "http://localhost:3001",
                            "http://localhost:3002",
                            "http://localhost:3003",
                            "http://localhost:3004",
                            "http://localhost:3005", "http://localhost");
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
        return tracingServiceName != null && ctx != null ? getTraceBasedRequestId(ctx) : UUID.randomUUID().toString();
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
        return currentTraceId + "@" + currentSpanId;
    }

}
