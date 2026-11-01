package com.bhf.aeroncache.ws.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.PrimeNearCacheResponse;
import com.bhf.aeroncache.http.responses.RequestErrorResponse;
import com.bhf.aeroncache.http.responses.StopNearCacheResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.near.ConcurrentNearCacheManager;
import com.bhf.aeroncache.near.NearCacheManager;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.ResponseChannelCacheConnector;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.ReconnectingAeronCache;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCountersRequestPublisher;
import com.bhf.aeroncache.services.cacheclient.CacheClientFactory;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.services.cluster.impl.RBClusterMessagePublisher;
import com.bhf.aeroncache.utils.*;
import com.bhf.aeroncache.ws.config.WsIdleStrategies;
import com.bhf.aeroncache.ws.bidi.BidiWsRouteHandler;
import com.bhf.aeroncache.ws.handlers.CacheWsRouteHandlers;
import com.bhf.aeroncache.ws.handlers.CountersWsRouteHandlers;
import io.aeron.Aeron;
import io.aeron.RethrowingErrorHandler;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.servlet.JavalinServletContext;
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
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Log4j2
public class WebsocketApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    public static final int TERM_SIZE_MULTIPLE = 4;
    @Setter
    private static int DEFAULT_WS_PORT = 7071;
    private static final String CACHE_API_PREFIX = "/api/ws/v1/cache/";
    private static final String CACHE_MULTI_SUB_API_PREFIX = "/api/ws/v1/caches/";
    private static final String COUNTERS_API_PREFIX = "/api/ws/v1/counter/";
    private static final String COUNTERS_MULTI_SUB_API_PREFIX = "/api/ws/v1/counters/";
    private static final String BIDI_API = "/api/ws/v1/bidi";
    private static final String NEAR_CACHES_API = "/api/v1/near/caches";
    private static final String NEAR_CACHE_API_PREFIX = "/api/v1/near/cache/";
    private static final String NEAR_COUNTERS_CACHES_API = "/api/v1/near/counters-caches";
    private static final String NEAR_COUNTERS_API_PREFIX = "/api/v1/near/counters/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static final boolean PRE_ENCODE_CACHE_REQUESTS = false;
    private static AeronCacheClusterListener client;
    private static CacheSubscriptionRequestPublisher subscriptionService;
    private static CacheSubscriptionRequestPublisher countersSubscriptionService;
    private static NearCacheManager<Object> nearCacheManager;
    private static NearCacheManager<Object> countersNearCacheManager;
    private static boolean nearCacheEnabled;
    private static AeronCache cache;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    public static final CacheStatsTracker statsTracker = new CacheStatsTracker();

    public static String tracingServiceName;

    private static AgentRunner agentRunner;
    private static MediaDriver mediaDriver;
    private static boolean CLUSTERED_MODE;
    public static int BOUND_PORT;

    public static void main(String[] args) {
        BOUND_PORT = startWebsocketInterface(DEFAULT_WS_PORT);
    }

    public static int startWebsocketInterface(int port) {
        System.out.println("Starting Websocket interface");
        tracingServiceName = System.getenv("OTEL_SERVICE_NAME");
        nearCacheEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("NEAR_CACHE_ENABLED", "false"));
        System.out.println("Near cache hydration enabled: " + nearCacheEnabled);
        nearCacheManager = nearCacheEnabled ? new ConcurrentNearCacheManager<>() : null;
        countersNearCacheManager = nearCacheEnabled ? new ConcurrentNearCacheManager<>() : null;

        var app = startHTTPServer(port);

        try {
            ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(ClusterUtils.getConfiguredTermLength(16777216* TERM_SIZE_MULTIPLE));
            System.out.println("Starting AeronCache Cluster Interface");

            CacheClientFactory clientFactory = getCacheClientFactory();
            var cacheRequestEncoder = clientFactory.getCacheRequestEncoder();
            var responseDecoder = clientFactory.getCacheResponseDecoder();
            var schemaDetailsProvider = clientFactory.getSchemaDetails();
            var indexSupplier = clientFactory.getIndexSupplier();
            var keySupplier = clientFactory.getKeySupplier();
            var valueSupplier = clientFactory.getValueSupplier();
            var countersResponseDecoder = clientFactory.getCountersResponseDecoder();
            var countersRequestEncoder = clientFactory.getCountersRequestEncoder();

            if (PRE_ENCODE_CACHE_REQUESTS) {
                // We encode the SBE messages before dropping them onto an Agrona RB for
                // sending directly to the cluster
                var requestPublisher = new RBClusterMessagePublisher(cache, rb,
                        WsIdleStrategies.clusterMessagePublisherIdleStrategy.get(), cacheRequestEncoder);
                subscriptionService = new CacheSubscriptionRequestPublisher(requestPublisher, nearCacheManager);
            } else {
                // Drop normalised cache requests onto an Agrona RB for encoding
                // to SBE on the Agent thread
                CacheRequestPublisher rbPublisher = new RBCacheRequestPublisher(rb);
                subscriptionService = new CacheSubscriptionRequestPublisher(rbPublisher, nearCacheManager);

                CacheRequestPublisher<String, String, Long> countersRbPublisher = new RBCountersRequestPublisher(rb);
                countersSubscriptionService = new CacheSubscriptionRequestPublisher(countersRbPublisher, countersNearCacheManager);
            }

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider, indexSupplier, keySupplier, valueSupplier);
            client.setCacheResultsCallbacks(subscriptionService);
            client.setCountersResultsCallbacks(countersSubscriptionService);
            client.setCountersCacheResponseDecoder(countersResponseDecoder);

            setupCacheWsRouteHandlers(app);
            setupCountersWsRouteHandlers(app);
            setupBidiWsRouteHandler(app);
            if (nearCacheEnabled) {
                setupNearCacheManagementRoutes(app);
            }

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
                buildUnclusteredConnection(aeron);
            }

            System.out.println("Building cluster agent for websocket service");
            var clusterClientAgentIdleStrategy = CLUSTERED_MODE ? WsIdleStrategies.clusterClientAgentIdleStrategy.get() : WsIdleStrategies.unclusteredIdleStrategy.get();
            var clusterMessagePublisherIdleStrategy = CLUSTERED_MODE ? WsIdleStrategies.clusterMessagePublisherIdleStrategy.get() : WsIdleStrategies.unclusteredIdleStrategy.get();
            var agentRunnerIdleStrategy = CLUSTERED_MODE ? WsIdleStrategies.agentRunnerIdleStrategy.get() : WsIdleStrategies.unclusteredIdleStrategy.get();

            var countersProtocolPublisher = new ClusterMessagePublisher<>(cache, clusterMessagePublisherIdleStrategy, countersRequestEncoder);

            var agent = PRE_ENCODE_CACHE_REQUESTS ?
                    new ClusterClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-CacheClient-Agent") :
                    new CacheClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), countersProtocolPublisher, "AeronCache-CacheClient-Agent");

            var errorHandler = new RethrowingErrorHandler();
            var errorCounter = (org.agrona.concurrent.status.AtomicCounter) null;
            agentRunner = new AgentRunner(agentRunnerIdleStrategy, errorHandler, errorCounter, agent);
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

    private static void buildUnclusteredConnection(Aeron aeron) {
        final AeronCacheClusterListener egressListener = client;
        final FragmentHandler egressFragmentHandler = (buffer, offset, length, header)
                -> egressListener.onMessage(header.sessionId(),
                System.currentTimeMillis(),
                buffer, offset, length, header);

        cache = ResponseChannelCacheConnector.connect(aeron,
                ResponseChannelCacheConnector.requestEndpoint(),
                ResponseChannelCacheConnector.requestStreamId(),
                ResponseChannelCacheConnector.responseControlEndpoint(),
                ResponseChannelCacheConnector.responseStreamId(),
                egressFragmentHandler,
                WsIdleStrategies.unclusteredIdleStrategy.get(),
                aeron.context().idleStrategy(),
                "AC-Unclustered-requests-listener");

        clusterConnected.set(true);
    }

    private static void buildClusterConnection(String egressIP, String ingressEndpoints, String aeronDirectory) {
        ReconnectingAeronCache reconnectingCache = new ReconnectingAeronCache(egressIP, ingressEndpoints, client, "WSClient",
                aeronDirectory, clusterConnected::set);
        reconnectingCache.connect();
        cache = reconnectingCache;
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
                .before(CACHE_API_PREFIX + "*", _ -> statsTracker.getTotalOpsCount().incrementAndGet())
                .get(LIVENESS, WebsocketApplication::handleGetLiveness)
                .get(READINESS, WebsocketApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .start(port);
    }

    /**
     * Register the websocket routes for regular cache subscriptions.
     *
     * @param app The Javalin instance.
     */
    private static void setupCacheWsRouteHandlers(Javalin app) {
        var handlers = new CacheWsRouteHandlers(subscriptionService);
        app.ws(CACHE_API_PREFIX + "hydrate/{cacheId}", handlers::handleSingleCacheWsWithHydration)
                .ws(CACHE_MULTI_SUB_API_PREFIX + "hydrate/{cacheIds}", handlers::handleMultiCacheWsWithHydration)
                .ws(CACHE_API_PREFIX + "{cacheId}", handlers::handleSingleCacheWs)
                .ws(CACHE_MULTI_SUB_API_PREFIX + "{cacheIds}", handlers::handleMultiCacheWs);
    }

    /**
     * Register the HTTP management routes for the near cache: list warm caches and stop near caching a
     * cache. Only registered when the near cache is enabled.
     *
     * @param app The Javalin instance.
     */
    private static void setupNearCacheManagementRoutes(Javalin app) {
        app.get(NEAR_CACHES_API, ctx -> handleGetNearCaches(ctx, subscriptionService))
                .post(NEAR_CACHE_API_PREFIX + "{cacheId}", ctx -> handlePrimeNearCache(ctx, subscriptionService))
                .delete(NEAR_CACHE_API_PREFIX + "{cacheId}", ctx -> handleStopNearCache(ctx, subscriptionService))
                .get(NEAR_COUNTERS_CACHES_API, ctx -> handleGetNearCaches(ctx, countersSubscriptionService))
                .post(NEAR_COUNTERS_API_PREFIX + "{cacheId}", ctx -> handlePrimeNearCache(ctx, countersSubscriptionService))
                .delete(NEAR_COUNTERS_API_PREFIX + "{cacheId}", ctx -> handleStopNearCache(ctx, countersSubscriptionService));
    }

    /**
     * List the caches currently kept warm in the given subscription service's near cache, with their
     * local item counts.
     *
     * @param ctx                 The context.
     * @param subscriptionService The subscription service whose near cache to list.
     */
    private static void handleGetNearCaches(Context ctx, CacheSubscriptionRequestPublisher subscriptionService) {
        log.info("Got request to list warm near caches");
        List<CacheDetails> warmCaches = new ArrayList<>();
        for (var cacheIdObj : subscriptionService.warmNearCaches()) {
            var cacheId = (String) cacheIdObj;
            warmCaches.add(new CacheDetails(cacheId, subscriptionService.nearCacheItemCount(cacheId)));
        }
        ctx.status(HTTPStatusUtils.OK);
        ctx.json(warmCaches);
    }

    /**
     * Stop near caching a cache: drop the internal subscription and remove the local copy. Active client
     * subscriptions are not affected.
     *
     * @param ctx                 The context.
     * @param subscriptionService The subscription service whose near cache to stop.
     */
    private static void handleStopNearCache(Context ctx, CacheSubscriptionRequestPublisher subscriptionService) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got request to stop near caching cacheId {}", cacheId);
            var wasCached = subscriptionService.stopNearCaching(cache, cacheId);
            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new StopNearCacheResponse(cacheId, wasCached, CacheOperationStatus.SUCCESS));
        } catch (Exception e) {
            var errorMsg = "Badly formed request to stop near caching cacheId " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Prime a cache in the given subscription service's near cache: start the internal near cache
     * subscription (requesting a snapshot) so the local copy is warmed up front, before any hydrating
     * client subscription arrives. Idempotent - priming a cache that is already warm succeeds with
     * {@code wasAlreadyWarm=true} and does not re-subscribe.
     *
     * @param ctx                 The context.
     * @param subscriptionService The subscription service whose near cache to prime.
     */
    private static void handlePrimeNearCache(Context ctx, CacheSubscriptionRequestPublisher subscriptionService) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got request to prime near caching cacheId {}", cacheId);
            var primed = subscriptionService.primeNearCache(cache, cacheId);
            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new PrimeNearCacheResponse(cacheId, !primed, CacheOperationStatus.SUCCESS));
        } catch (Exception e) {
            var errorMsg = "Badly formed request to prime near caching cacheId " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Register the websocket routes for counter cache subscriptions.
     *
     * @param app The Javalin instance.
     */
    private static void setupCountersWsRouteHandlers(Javalin app) {
        var handlers = new CountersWsRouteHandlers(countersSubscriptionService);
        app.ws(COUNTERS_API_PREFIX + "hydrate/{cacheId}", handlers::handleSingleCacheWsWithHydration)
                .ws(COUNTERS_MULTI_SUB_API_PREFIX + "hydrate/{cacheIds}", handlers::handleMultiCacheWsWithHydration)
                .ws(COUNTERS_API_PREFIX + "{cacheId}", handlers::handleSingleCacheWs)
                .ws(COUNTERS_MULTI_SUB_API_PREFIX + "{cacheIds}", handlers::handleMultiCacheWs);
    }

    /**
     * Register the single bidirectional websocket endpoint that carries the full command surface plus
     * dynamic subscribe/unsubscribe, correlated per request. The one-directional subscription routes
     * remain available unchanged.
     *
     * @param app The Javalin instance.
     */
    private static void setupBidiWsRouteHandler(Javalin app) {
        var handler = new BidiWsRouteHandler(subscriptionService, countersSubscriptionService);
        app.ws(BIDI_API, handler::handleBidi);
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
     * The current cluster connection used to send subscription requests.
     *
     * @return The cache cluster connection.
     */
    public static AeronCache getCache() {
        return cache;
    }

    /**
     * Whether the service is running against a Raft cluster (as opposed to unclustered mode).
     *
     * @return {@code true} if clustered mode is in use.
     */
    public static boolean isClusteredMode() {
        return CLUSTERED_MODE;
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

}
