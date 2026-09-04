package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.config.HttpIdleStrategies;
import com.bhf.aeroncache.http.handlers.CacheRouteHandlers;
import com.bhf.aeroncache.http.handlers.CountersRouteHandlers;
import com.bhf.aeroncache.http.handlers.HTTPConsumerUtils;
import com.bhf.aeroncache.http.requests.ClusterToolsRequest;
import com.bhf.aeroncache.http.responses.CacheDetails;
import com.bhf.aeroncache.http.responses.ClusterToolsResponse;
import com.bhf.aeroncache.http.responses.RequestErrorResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.responses.BulkCacheOpsResponse;
import com.bhf.aeroncache.models.results.BulkCacheOpsResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.services.ReconnectingAeronCache;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCountersRequestPublisher;
import com.bhf.aeroncache.services.cacheclient.CacheClientFactory;
import com.bhf.aeroncache.services.cluster.BlockingClusterRequestPublisher;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.services.cluster.impl.ObservingClusterRequestPublisher;
import com.bhf.aeroncache.services.cluster.impl.RBClusterMessagePublisher;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import com.bhf.aeroncache.utils.RingBufferUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.bhf.aeroncache.http.handlers.AbstractRouteHandlers.getRequestId;

@Log4j2
public class HttpApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    @Setter
    private static int CLUSTER_TOOLS_PORT = 7080;
    @Setter
    private static int DEFAULT_HTTP_PORT = 7070;
    private static final String DEFAULT_CLUSTER_TOOLS_ENDPOINT = "http://localhost:7080/api/v1/clustertools/";
    private static final String CACHE_API_PREFIX = "/api/v1/cache/";
    private static final String COUNTERS_API_PREFIX = "/api/v1/counters/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static final boolean PRE_ENCODE_CACHE_REQUESTS = false;

    private static AeronCacheClusterListener<ReusableString, ReusableString, ReusableString> client;
    @Getter
    private static ObservingCacheRequestPublisher<ReusableString, ReusableString, ReusableString, String, String, String> cachePublisher;
    private static ObservingCacheRequestPublisher<ReusableString, ReusableString, ReusableLong, String, String, Long> countersPublisher;
    private static AeronCache cache;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    public static final CacheStatsTracker statsTracker = new CacheStatsTracker();
    public static final Set<String> allCaches = new ConcurrentSkipListSet<>();
    public static final Map<String, Long> cacheToSize = new ConcurrentHashMap<>();

    public static String tracingServiceName;
    private static AgentRunner agentRunner;
    private static MediaDriver mediaDriver;


    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static List<String> hostArray = new ArrayList<>();
    public static int BOUND_PORT;

    public static void main(String[] args) {
        var cacheMode = System.getenv("CACHE_MODE");
        final boolean CLUSTERED_MODE = cacheMode == null || cacheMode.toUpperCase().equals("RAFT");

        System.out.println("Cache mode: " + cacheMode + ", using clustered mode: " + CLUSTERED_MODE);
        BOUND_PORT = startHTTPInterface(DEFAULT_HTTP_PORT, CLUSTERED_MODE);
    }

    public static int startHTTPInterface(int port, boolean useClusteredMode) {

        System.out.println("Starting HTTP interface");
        tracingServiceName = System.getenv("OTEL_SERVICE_NAME");

        var app = startHTTPServer(port);

        try {
            final ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);
            System.out.println("Starting AeronCache Cluster Interface");

            CacheClientFactory clientFactory = getCacheClientFactory();
            var cacheRequestEncoder = clientFactory.getCacheRequestEncoder();
            var responseDecoder = clientFactory.getCacheResponseDecoder();
            var countersRequestEncoder = clientFactory.getCountersRequestEncoder();
            var countersResponseDecoder = clientFactory.getCountersResponseDecoder();
            var schemaDetailsProvider = clientFactory.getSchemaDetails();
            var indexSupplier = clientFactory.getIndexSupplier();
            var keySupplier = clientFactory.getKeySupplier();
            var valueSupplier = clientFactory.getValueSupplier();

            if (PRE_ENCODE_CACHE_REQUESTS) {
                // We encode the SBE messages before dropping them onto an Agrona RB for
                // sending directly to the cluster
                CacheRequestPublisher<String, String, String> cacheRequestPublisher = new RBClusterMessagePublisher(cache, rb,
                        HttpIdleStrategies.clusterMessagePublisherIdleStrategy.get(), cacheRequestEncoder);

                BlockingClusterRequestPublisher blockingRequestPublisher = new ClusterMessagePublisher(cache,
                        HttpIdleStrategies.blockingPublisherIdleStrategy.get(), clientFactory.getCacheRequestEncoder());
                cachePublisher = new ObservingClusterRequestPublisher(cacheRequestPublisher,
                        blockingRequestPublisher);
            } else {
                // Drop normalised cache requests onto an Agrona RB for encoding
                // to SBE on the Agent thread
                CacheRequestPublisher<String, String, String> rbPublisher = new RBCacheRequestPublisher(rb);
                cachePublisher = new ObservingCacheRequestPublisher(rbPublisher);

                CacheRequestPublisher<String, String, Long> countersRbPublisher = new RBCountersRequestPublisher(rb);
                countersPublisher = new ObservingCacheRequestPublisher<>(countersRbPublisher);
            }

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider, indexSupplier, keySupplier, valueSupplier);
            client.setCacheResultsCallbacks(cachePublisher);
            client.setCountersResultsCallbacks(countersPublisher);
            client.setCountersCacheResponseDecoder(countersResponseDecoder);

            setupCacheRouteHandlers(app);
            setupCacheCountersRouteHandlers(app);

            var podName = System.getenv("POD_ADDRESS");
            var allHosts = System.getenv("CLUSTER_ADDRESSES");

            System.out.println("POD_ADDRESS=" + podName);
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);

            var egressIP = DNSUtils.getThisHostName();
            hostArray = allHosts!=null ? List.of(allHosts.split(",")) : List.of("localhost");
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

            if (useClusteredMode) {
                buildClusterConnection(egressIP, ingressEndpoints, aeronCtx.aeronDirectoryName());
            } else {

                final Aeron aeron = Aeron.connect(aeronCtx);
                var requestPubHost = System.getenv("REQUEST_PUB_HOST");
                buildUnclusteredConnection(aeron, requestPubHost);
            }

            System.out.println("Building cluster agent for http service, cluster connected: "+clusterConnected.get());
            var clusterClientAgentIdleStrategy = HttpIdleStrategies.clusterClientAgentIdleStrategy.get();
            var clusterMessagePublisherIdleStrategy = HttpIdleStrategies.clusterMessagePublisherIdleStrategy.get();
            var cacheProtocolPublisher = new ClusterMessagePublisher(cache,
                    clusterMessagePublisherIdleStrategy, cacheRequestEncoder);

            var countersProtocolPublisher = new ClusterMessagePublisher<>(cache, clusterMessagePublisherIdleStrategy, countersRequestEncoder);

            var agent = PRE_ENCODE_CACHE_REQUESTS ?
                    new ClusterClientAgent(cache, rb, clusterClientAgentIdleStrategy, cacheProtocolPublisher, "AeronCache-ClusterClient-Agent") :
                    new CacheClientAgent(cache, rb, clusterClientAgentIdleStrategy, cacheProtocolPublisher, countersProtocolPublisher, "AeronCache-CacheClient-Agent");

            var errorHandler = new RethrowingErrorHandler();
            var errorCounter = (org.agrona.concurrent.status.AtomicCounter) null;
            final IdleStrategy agentRunnerIdleStrategy = HttpIdleStrategies.agentRunnerIdleStrategy.get();
            agentRunner = new AgentRunner(agentRunnerIdleStrategy, errorHandler, errorCounter, agent);
            AgentRunner.startOnThread(agentRunner);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        var httpPort = app.port();
        log.info("Started HTTP interface on port "+httpPort);
        return httpPort;
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

        var requestPublicationChannel = "aeron:udp?endpoint=" + requestPubHost + ":8008|alias=AC-unclustered-requests";
        int requestPublicationStream = 1;
        var requestPublication = aeron.addPublication(requestPublicationChannel,
                requestPublicationStream);

        var hostname = DNSUtils.getThisHostName();
        var responseSubscriptionChannel = "aeron:udp?endpoint=" + hostname + ":8007|alias=AC-unclustered-responses";
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
                return responseSubscription.poll(egressFragmentHandler, Integer.MAX_VALUE);
            }

            @Override
            public String roleName() {
                return "AC-Unclustered-requests-listener";
            }
        };

        IdleStrategy unclusteredAgentIdleStrategy = HttpIdleStrategies.unclusteredIdleStrategy.get();
        final AgentRunner serverAgentRunner = new AgentRunner(unclusteredAgentIdleStrategy,
                Throwable::printStackTrace,
                null, serverAgent);

        AgentRunner.startOnThread(serverAgentRunner);

        clusterConnected.set(true);
    }

    private static void buildClusterConnection(String egressIP, String ingressEndpoints, String aeronDirectory) {
        ReconnectingAeronCache reconnectingCache = new ReconnectingAeronCache(egressIP, ingressEndpoints, client, "HTTPClient",
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
        registry.config().commonTags("application", "aeron-cache-http");

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
                .beforeMatched(HttpApplication::checkClusterConnectivity)
                .before(CACHE_API_PREFIX + "*", _ -> statsTracker.getTotalOpsCount().incrementAndGet())
                .post(CACHE_API_PREFIX + "bulkops/", HttpApplication::handleBulkOpsRequest)
                .post("/api/v1/shutdown", HttpApplication::handleShutdownCluster)
                .post("/api/v1/snapshot", HttpApplication::handleTakeSnapshot)
                .get(LIVENESS, HttpApplication::handleGetLiveness)
                .get(READINESS, HttpApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .post("baselinePost", HttpApplication::postActionBaseline)
                .get("baselineGet", HttpApplication::getActionBaseline)
                .start(port);
    }

    private static void setupCacheCountersRouteHandlers(Javalin app) {
        var countersHandlers = new CountersRouteHandlers(countersPublisher);
        app.post(COUNTERS_API_PREFIX, countersHandlers::handleCreateCacheRequest)
                .get(COUNTERS_API_PREFIX + "<cacheId>/<key>", countersHandlers::handleGetItemRequest)
                .get(COUNTERS_API_PREFIX + "<cacheId>", countersHandlers::handleGetCacheRequest)
                .post(COUNTERS_API_PREFIX + "timed/<cacheId>", countersHandlers::handlePutTimedItemRequest)
                .post(COUNTERS_API_PREFIX + "increment/<cacheId>", countersHandlers::handleIncrementItemRequest)
                .post(COUNTERS_API_PREFIX + "decrement/<cacheId>", countersHandlers::handleDecrementItemRequest)
                .post(COUNTERS_API_PREFIX + "<cacheId>", countersHandlers::handlePutItemRequest)
                .delete(COUNTERS_API_PREFIX + "<cacheId>/<key>", countersHandlers::handleDeleteItemRequest)
                .delete(COUNTERS_API_PREFIX + "<cacheId>", countersHandlers::handleDeleteCacheRequest)
                .patch(COUNTERS_API_PREFIX + "<cacheId>", countersHandlers::handleClearCacheRequest)
                .get("/api/v1/counters-caches", countersHandlers::handleGetCachesRequest)
                .get("/api/v1/counters-stats", countersHandlers::handleGetStatsRequest);
    }

    private static void setupCacheRouteHandlers(Javalin app) {
        var handlers = new CacheRouteHandlers(cachePublisher);
        app.post(CACHE_API_PREFIX, handlers::handleCreateCacheRequest)
                .get(CACHE_API_PREFIX + "<cacheId>/<key>", handlers::handleGetItemRequest)
                .get(CACHE_API_PREFIX + "<cacheId>", handlers::handleGetCacheRequest)
                .post(CACHE_API_PREFIX + "timed/<cacheId>", handlers::handlePutTimedItemRequest)
                .post(CACHE_API_PREFIX + "<cacheId>", handlers::handlePutItemRequest)
                .delete(CACHE_API_PREFIX + "<cacheId>/<key>", handlers::handleDeleteItemRequest)
                .delete(CACHE_API_PREFIX + "<cacheId>", handlers::handleDeleteCacheRequest)
                .patch(CACHE_API_PREFIX + "<cacheId>", handlers::handleClearCacheRequest)
                .get("/api/v1/caches", handlers::handleGetCachesRequest)
                .get("/api/v1/stats", handlers::handleGetStatsRequest);
    }

    /**
     * Handle a request to perform bulk cache operations.
     *
     * @param ctx The context.
     */
    public static void handleBulkOpsRequest(@NotNull Context ctx) {
        try {
            var request = ctx.bodyAsClass(BulkCacheOpsRequest.class);
            log.info("Got bulk cache ops request: {}", request);

            var requestId = getRequestId(ctx);
            CompletableFuture<BulkCacheOpsResponse> future = new CompletableFuture<>();
            Consumer<BulkCacheOpsResult<ReusableString, ReusableString, ReusableString>> consumer = HTTPConsumerUtils.getBulkCacheOpsResultConsumer(future, request.requestId());

            CompletableFuture.runAsync(() -> HttpApplication.getCachePublisher().sendBulkOperationsRequest(requestId, request, consumer));

            var response = future.get();

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed bulk operation request: " + ctx.body();
            log.warn(errorMsg);
            HttpApplication.statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    private static void handleShutdownCluster(@NotNull Context context) {
        log.info("Got shutdown cluster request");
        makeClusterToolsRequest(context, "shutdown");
    }

    private static void handleTakeSnapshot(@NotNull Context context) {
        log.info("Got take snapshot request");
        makeClusterToolsRequest(context, "snapshot");
    }

    private static void makeClusterToolsRequest(Context context, String command) {
        var clusterToolsFolder = System.getenv().getOrDefault("CLUSTER_FOLDER", "node0/cluster");
        var requestBody = new ClusterToolsRequest(command, clusterToolsFolder);

        try {
            var jsonBody = OBJECT_MAPPER.writeValueAsString(requestBody);

            for (String host : hostArray) {
                try {
                    String hostUri = "http://" + host + ":"+CLUSTER_TOOLS_PORT+"/api/v1/clustertools/";
                    log.info("Sending {} request to host: {}", command, hostUri);

                    var httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(hostUri))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> httpResponse = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                    if (httpResponse.statusCode() == HTTPStatusUtils.OK) {
                        var response = OBJECT_MAPPER.readValue(httpResponse.body(), ClusterToolsResponse.class);

                        if(response.exitCode()==0) {
                            context.status(HTTPStatusUtils.OK);
                            context.json(response);
                            break;
                        }
                    } else {
                        log.error("Cluster tools request failed for host {} with status code: {}", host, httpResponse.statusCode());
                    }
                } catch (IOException e) {
                    log.error("Cluster tools request failed for host {}", host);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            context.status(HTTPStatusUtils.OK);
            ClusterToolsResponse response = new ClusterToolsResponse(command, clusterToolsFolder, -1);
            context.json(response);
        } catch (Exception e) {
            log.error("Error making cluster tools request", e);
            context.status(HTTPStatusUtils.BAD_REQUEST);
            var errorResponse = new RequestErrorResponse("Error making cluster tools request for " + command, e.getMessage(),
                    CacheOperationStatus.ERROR);
            context.json(errorResponse);
        }
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
     * Used to establish a performance baseline for POST requests.
     *
     * @param ctx
     */
    private static void postActionBaseline(Context ctx) {
        statsTracker.getTotalOpsCount().incrementAndGet();
    }

    /**
     * Used to establish a performance baseline for GET requests.
     *
     * @param ctx
     */
    private static void getActionBaseline(Context ctx) {
        statsTracker.getTotalOpsCount().incrementAndGet();
    }


    /**
     * Handle getting details of available caches. Currently only
     * implemented in memory on the HTTP side. Information returned is
     * populated from any previous call to get the stats from the
     * Aeron Cache instance.
     *
     * @param context The context.
     */
    private static void handleGetCachesRequest(Context context) {
        log.info("Got request to get all cache details");
        List<CacheDetails> cacheDetails = new ArrayList<>();
        for (var l : allCaches) {
            var itemCount = cacheToSize.getOrDefault(l, 0L);
            cacheDetails.add(new CacheDetails(l, itemCount));
        }
        context.json(cacheDetails);
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


}
