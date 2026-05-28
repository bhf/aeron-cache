package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.responses.CacheOperationResponse;
import com.bhf.aeroncache.http.config.HttpIdleStrategies;
import com.bhf.aeroncache.http.requests.ClusterToolsRequest;
import com.bhf.aeroncache.http.requests.CreateCacheRequest;
import com.bhf.aeroncache.http.requests.PutItemRequest;
import com.bhf.aeroncache.http.requests.PutTimedItemRequest;
import com.bhf.aeroncache.http.responses.*;
import com.bhf.aeroncache.http.responses.CacheStats;
import com.bhf.aeroncache.models.bulk.responses.BulkCacheOpsResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
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
import io.aeron.cluster.client.AeronCluster;
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
import io.opentelemetry.api.trace.Span;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
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
import java.util.regex.Pattern;

@Log4j2
public class HttpApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    @Setter
    private static int CLUSTER_TOOLS_PORT = 7080;
    @Setter
    private static int DEFAULT_HTTP_PORT = 7070;
    private static final String DEFAULT_CLUSTER_TOOLS_ENDPOINT = "http://localhost:7080/api/v1/clustertools/";
    private static final String API_PREFIX = "/api/v1/cache/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static final boolean PRE_ENCODE_CACHE_REQUESTS = false;

    private static AeronCacheClusterListener client;
    private static ObservingCacheRequestPublisher observingPublisher;
    private static AeronCache cache;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    private static final CacheStatsTracker statsTracker = new CacheStatsTracker();
    private final static Set<String> allCaches = new ConcurrentSkipListSet<>();
    private final static Map<String, Long> cacheToSize = new ConcurrentHashMap<>();

    private static String tracingServiceName;
    private static AgentRunner agentRunner;
    private static AeronCluster aeronCluster;
    private static MediaDriver mediaDriver;

    private static final Pattern specialCharacters = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!]");

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
            var schemaDetailsProvider = clientFactory.getSchemaDetails();
            var indexSupplier = clientFactory.getIndexSupplier();
            var keySupplier = clientFactory.getKeySupplier();
            var valueSupplier = clientFactory.getValueSupplier();

            if (PRE_ENCODE_CACHE_REQUESTS) {
                // We encode the SBE messages before dropping them onto an Agrona RB for
                // sending directly to the cluster
                CacheRequestPublisher cacheRequestPublisher = new RBClusterMessagePublisher(cache, rb,
                        HttpIdleStrategies.clusterMessagePublisherIdleStrategy.get(), cacheRequestEncoder);

                BlockingClusterRequestPublisher blockingRequestPublisher = new ClusterMessagePublisher(cache,
                        HttpIdleStrategies.blockingPublisherIdleStrategy.get(), clientFactory.getCacheRequestEncoder());
                observingPublisher = new ObservingClusterRequestPublisher(cacheRequestPublisher,
                        blockingRequestPublisher);
            } else {
                // Drop normalised cache requests onto an Agrona RB for encoding
                // to SBE on the Agent thread
                CacheRequestPublisher rbPublisher = new RBCacheRequestPublisher(rb);
                observingPublisher = new ObservingCacheRequestPublisher(rbPublisher);
            }

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider, indexSupplier, keySupplier, valueSupplier);
            client.setCacheResultsCallbacks(observingPublisher);

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

            System.out.println("Building cluster agent for http service");
            var clusterClientAgentIdleStrategy = HttpIdleStrategies.clusterClientAgentIdleStrategy.get();
            var clusterMessagePublisherIdleStrategy = HttpIdleStrategies.clusterMessagePublisherIdleStrategy.get();
            var agent = PRE_ENCODE_CACHE_REQUESTS ?
                    new ClusterClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-ClusterClient-Agent") :
                    new CacheClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-CacheClient-Agent");

            var errorHandler = aeronCluster != null ? ClusterUtils.getAgentRunnerErrorHandler(aeronCluster) :
                    new RethrowingErrorHandler();
            var errorCounter = aeronCluster != null ? ClusterUtils.getAgentErrorCounter(aeronCluster, "HTTPClient") :
                    null;
            final IdleStrategy agentRunnerIdleStrategy = HttpIdleStrategies.agentRunnerIdleStrategy.get();
            agentRunner = new AgentRunner(agentRunnerIdleStrategy, errorHandler, errorCounter, agent);
            clusterConnected.set(true);
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
    }

    private static void buildClusterConnection(String egressIP, String ingressEndpoints, String aeronDirectory) {
        try {
            aeronCluster = ClusterUtils.buildClusterConnection(egressIP, ingressEndpoints, client, "HTTPClient",
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
                .before(API_PREFIX + "*", _ -> statsTracker.getTotalOpsCount().incrementAndGet())
                .post(API_PREFIX, HttpApplication::handleCreateCacheRequest)
                .post(API_PREFIX+"bulkops/", HttpApplication::handleBulkOpsRequest)
                .get(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleGetItemRequest)
                .get(API_PREFIX + "<cacheId>", HttpApplication::handleGetCacheRequest)
                .post(API_PREFIX + "timed/<cacheId>", HttpApplication::handlePutTimedItemRequest)
                .post(API_PREFIX + "<cacheId>", HttpApplication::handlePutItemRequest)
                .delete(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleDeleteItemRequest)
                .delete(API_PREFIX + "<cacheId>", HttpApplication::handleDeleteCacheRequest)
                .patch(API_PREFIX + "<cacheId>", HttpApplication::handleClearCacheRequest)
                .get("/api/v1/caches", HttpApplication::handleGetCachesRequest)
                .get("/api/v1/stats", HttpApplication::handleGetStatsRequest)
                .post("/api/v1/shutdown", HttpApplication::handleShutdownCluster)
                .post("/api/v1/snapshot", HttpApplication::handleTakeSnapshot)
                .get(LIVENESS, HttpApplication::handleGetLiveness)
                .get(READINESS, HttpApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .post("baselinePost", HttpApplication::postActionBaseline)
                .get("baselineGet", HttpApplication::getActionBaseline)
                .start(port);
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
        var clusterToolsFolder = System.getenv().getOrDefault("CLUSTER_FOLDER", "/tmp/aeron-cluster");
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

    private static void handleGetStatsRequest(Context ctx) {
        log.info("Got request to get cache stats");

        try {
            var requestId = getRequestId(ctx);
            CompletableFuture<CacheStats> future = new CompletableFuture<>();
            Consumer<CacheStatsResult> consumer = getCacheStatsResultConsumer(future);

            CompletableFuture.runAsync(() -> observingPublisher.getAllCacheStats(requestId, consumer));
            var response = future.get();

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get cache stats";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<CacheStatsResult> getCacheStatsResultConsumer(CompletableFuture<CacheStats> future) {
        Consumer<CacheStatsResult> consumer = c -> {
            {
                log.info("Got cache stats, requestId {}", c.getRequestId());
                int totalOps = statsTracker.getTotalOpsCount().get();
                int totalCaches = 0;
                int totalItems = 0;

                List<com.bhf.aeroncache.models.results.CacheStats> stats = c.getStats();
                Set<String> latestCaches = new HashSet<>();
                for (var x : stats) {
                    totalCaches++;
                    totalItems += x.size;
                    var cacheId = x.getCacheId().value().toString();
                    latestCaches.add(cacheId);
                    cacheToSize.put(cacheId, x.size);
                }
                
                allCaches.retainAll(latestCaches);
                allCaches.addAll(latestCaches);

                var statsTrackerStats = statsTracker.getCacheStats();
                var response = new CacheStats(totalOps, totalCaches, totalItems, statsTrackerStats.errorCount());
                future.complete(response);
            }
        };
        return consumer;
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

    /**
     * Handle a request to delete a cache.
     *
     * @param ctx The context.
     */
    private static void handleDeleteCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got delete cache request for cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<DeleteCacheResponse> future = new CompletableFuture<>();
            Consumer<DeleteCacheResult> consumer = getDeleteCacheResultConsumer(future);
            CompletableFuture.runAsync(() -> observingPublisher.deleteCache(requestId, cacheId, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                allCaches.remove(response.cacheId());
                statsTracker.getTotalCaches().decrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to delete cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<DeleteCacheResult> getDeleteCacheResultConsumer(CompletableFuture<DeleteCacheResponse> future) {
        Consumer<DeleteCacheResult> consumer = c -> {

            var deletedCacheId = c.getCacheId();
            log.info("Got delete cache response from cluster on cacheId {}", deletedCacheId);
            var response = new DeleteCacheResponse(deletedCacheId.value().toString(), c.getStatus());
            future.complete(response);
        };
        return consumer;
    }


    /**
     * Handle a request to delete an item from a cache.
     *
     * @param ctx The context.
     */
    private static void handleDeleteItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got delete item request on cacheId {}, key {}",
                    cacheId, key);

            var requestId = getRequestId(ctx);

            CompletableFuture<DeleteItemResponse> future = new CompletableFuture<>();
            Consumer<RemoveCacheEntryResult> consumer = getRemoveCacheEntryResultConsumer(future);
            CompletableFuture.runAsync(() -> observingPublisher.removeCacheEntry(requestId, cacheId, key, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                statsTracker.getTotalItems().decrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg =
                    "Badly formed request to delete item with key " + ctx.pathParam("key") + " from cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<RemoveCacheEntryResult> getRemoveCacheEntryResultConsumer(CompletableFuture<DeleteItemResponse> future) {
        Consumer<RemoveCacheEntryResult> consumer = c -> {

            log.info("Got delete item response from cluster on cacheId {}, key {}", c.getCacheId(), c.getKey());
            var response = new DeleteItemResponse(c.getCacheId().value().toString(), c.getKey().value().toString(), c.getStatus());
            future.complete(response);
        };
        return consumer;
    }

    /**
     * Handle a request to clear a cache.
     *
     * @param ctx The context.
     */
    private static void handleClearCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got clear request on cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<ClearCacheResponse> future = new CompletableFuture<>();
            Consumer<ClearCacheResult> consumer = getClearCacheResultConsumer(cacheId, future);
            CompletableFuture.runAsync(() -> observingPublisher.clearCache(requestId, cacheId, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to clear cache with ID " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<ClearCacheResult> getClearCacheResultConsumer(String cacheId, CompletableFuture<ClearCacheResponse> future) {
        Consumer<ClearCacheResult> consumer = c -> {

            log.info("Got clear cache response from cluster on cacheId {}", c.getCacheId());
            var response = new ClearCacheResponse(cacheId, c.getStatus());
            future.complete(response);
        };
        return consumer;
    }

    /**
     * Handle a request to get an item from a cache.
     *
     * @param ctx The context.
     */
    private static void handleGetItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var key = ctx.pathParam("key");
            log.info("Got get item request on cacheId {}, key {}",
                    cacheId, key);

            var requestId = getRequestId(ctx);
            CompletableFuture<GetItemResponse> future = new CompletableFuture<>();
            Consumer<GetCacheEntryResult> consumer = getGetCacheEntryResultConsumer(future);
            CompletableFuture.runAsync(() -> observingPublisher.getCacheEntry(requestId, cacheId, key, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg =
                    "Badly formed request to get item with key " + ctx.pathParam("key") + " from cache with Id: " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<GetCacheEntryResult> getGetCacheEntryResultConsumer(CompletableFuture<GetItemResponse> future) {
        Consumer<GetCacheEntryResult> consumer = c -> {

            log.info("Get item response from cluster on cacheId {}, key {}, value {}", c.getCacheId(),
                    c.getEntryKey(), c.getEntryValue());
            var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
            var response = noCache ?
                    new GetItemResponse("0", "NA", "NA", c.getStatus()) :
                    new GetItemResponse(c.getCacheId().value().toString(), c.getEntryKey().value().toString(),
                            c.getEntryValue().value().toString(), c.getStatus());
            future.complete(response);
        };
        return consumer;
    }

    /**
     * Handle a request to add an item to a cache.
     *
     * @param ctx The context.
     */
    private static void handlePutItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var request = ctx.bodyAsClass(PutItemRequest.class);
            log.info("Got put item request on cacheId {}, key {}, value {}",
                    cacheId, request.key(), request.value());

            var requestId = getRequestId(ctx);
            CompletableFuture<PutItemResponse> future = new CompletableFuture<>();
            Consumer<AddCacheEntryResult> consumer = getAddCacheEntryResultConsumer(request.key(), future);
            long ttl = 0;
            CompletableFuture.runAsync(() -> observingPublisher.addCacheEntry(requestId, cacheId,
                    request.key(), request.value(), ttl, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                statsTracker.getTotalItems().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to put item from request: " + ctx.body();
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to add a timed item to a cache.
     *
     * @param ctx The context.
     */
    private static void handlePutTimedItemRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            var request = ctx.bodyAsClass(PutTimedItemRequest.class);
            log.info("Got put item request on cacheId {}, key {}, value {}, ttl {}",
                    cacheId, request.key(), request.value(), request.ttl());

            var requestId = getRequestId(ctx);
            CompletableFuture<PutItemResponse> future = new CompletableFuture<>();
            Consumer<AddCacheEntryResult> consumer = getAddCacheEntryResultConsumer(request.key(), future);
            long ttl = request.ttl();
            CompletableFuture.runAsync(() -> observingPublisher.addCacheEntry(requestId, cacheId,
                    request.key(), request.value(), ttl, consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                statsTracker.getTotalItems().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to put timed item from request: " + ctx.body();
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<AddCacheEntryResult> getAddCacheEntryResultConsumer(String key, CompletableFuture<PutItemResponse> future) {
        Consumer<AddCacheEntryResult> consumer = c -> {

            var cacheId = c.getCacheId();
            log.info("Got put item response from cluster on cacheId {}", cacheId);
            var response = new PutItemResponse(cacheId.value().toString(), key, c.getStatus());
            future.complete(response);
        };
        return consumer;
    }

    private static void handleBulkOpsRequest(@NotNull Context ctx) {
        try {
            var request = ctx.bodyAsClass(BulkCacheOpsRequest.class);
            log.info("Got bulk cache ops request: {}", request);

            var requestId = getRequestId(ctx);
            CompletableFuture<BulkCacheOpsResponse> future = new CompletableFuture<>();
            Consumer<BulkCacheOpsResult> consumer = getBulkCacheOpsResultConsumer(future, request.requestId());

            CompletableFuture.runAsync(() -> observingPublisher.sendBulkOperationsRequest(requestId, request, consumer));

            var response = future.get();

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed bulk operation request: " + ctx.body();
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<BulkCacheOpsResult> getBulkCacheOpsResultConsumer(CompletableFuture<BulkCacheOpsResponse> future, String requestId) {
        return c -> {
            List<CacheOperationResponse> operationResponses = new ArrayList<>();

            List<CacheOperationResultDetails<ReusableString, ReusableString, ReusableString>> ops = c.getOperations();
            for(var o : ops){
                var opRequestId = o.getRequestId();
                var cacheId = o.getCacheId();
                var value = o.getValue();
                var key = o.getKey();
                var status = o.getOperationStatus();
                operationResponses.add(new CacheOperationResponse(opRequestId, status, cacheId.value(), key.value(), value.value()));
            }

            BulkCacheOpsResponse response = new BulkCacheOpsResponse(requestId, operationResponses);
            future.complete(response);
        };
    }

    /**
     * Handle a request to create a cache.
     *
     * @param ctx The context.
     */
    private static void handleCreateCacheRequest(Context ctx) {
        try {
            var request = ctx.bodyAsClass(CreateCacheRequest.class);
            log.info("Got create cache request on cacheId {}", request.cacheId());

            if (specialCharacters.matcher(request.cacheId()).find()) {
                var errorMsg = "Cache ID shouldn't contain special characters";
                var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CACHE_ID_NO_SPECIAL_CHARACTERS,
                        CacheOperationStatus.ERROR);
                ctx.status(HTTPStatusUtils.BAD_REQUEST);
                ctx.json(badRequest);
                return;
            }

            var requestId = getRequestId(ctx);

            CompletableFuture<CreateCacheResponse> future = new CompletableFuture<>();
            Consumer<CreateCacheResult> consumer = getCreateCacheResultConsumer(future);
            CompletableFuture.runAsync(() -> observingPublisher.sendCreateCache(requestId, request.cacheId(), consumer));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                allCaches.add(response.cacheId());
                statsTracker.getTotalCaches().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to create cache from request: " + ctx.body();
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<CreateCacheResult> getCreateCacheResultConsumer(CompletableFuture<CreateCacheResponse> future) {
        Consumer<CreateCacheResult> consumer = new Consumer<CreateCacheResult>() {
            @Override
            public void accept(CreateCacheResult c) {

                var cacheId = c.getCacheId();
                log.info("Got create cache response from cluster on cacheId {}", cacheId);
                var response = new CreateCacheResponse(cacheId.value().toString(), c.getStatus());
                future.complete(response);
            }
        };
        return consumer;
    }

    /**
     * Handle a request to get a whole cache.
     *
     * @param ctx The context.
     */
    private static void handleGetCacheRequest(Context ctx) {
        try {
            var cacheId = ctx.pathParam("cacheId");
            log.info("Got get cache content request on cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<GetCacheResponse> future = new CompletableFuture<>();
            Consumer<GetAllCacheEntriesResult> consumer = getGetAllCacheEntriesResultConsumer(future);
            CompletableFuture.runAsync(() -> observingPublisher.getCacheEntries(requestId, cacheId, consumer));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get cache content for cache ID " + ctx.pathParam("cacheId");
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    @NotNull
    private static Consumer<GetAllCacheEntriesResult> getGetAllCacheEntriesResultConsumer(CompletableFuture<GetCacheResponse> future) {
        Consumer<GetAllCacheEntriesResult> consumer = c -> {

            log.info("Get cache content response from cluster on cacheId {}", c.getCacheId());
            var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
            var response = noCache ?
                    new GetCacheResponse("0", CacheOperationStatus.UNKNOWN_CACHE, List.of()) :
                    new GetCacheResponse("0", c.getStatus(), buildItemsList(c));
            future.complete(response);
        };
        return consumer;
    }

    private static List<CacheItem> buildItemsList(GetAllCacheEntriesResult<ReusableString, ReusableString,
            ReusableString> c) {
        List<CacheItem> res = new ArrayList<>();
        c.getValues().forEach((key, value) -> {
            res.add(new CacheItem(key.value(), value.value()));
        });
        return res;
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
        return currentTraceId + "@" + currentSpanId;
    }

}
