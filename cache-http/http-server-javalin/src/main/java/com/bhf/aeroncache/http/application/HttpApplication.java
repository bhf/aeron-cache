package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.codecs.request.RegularStringCacheRequestEncoder;
import com.bhf.aeroncache.http.config.HttpIdleStrategies;
import com.bhf.aeroncache.http.requests.CreateCacheRequest;
import com.bhf.aeroncache.http.requests.PutItemRequest;
import com.bhf.aeroncache.http.responses.*;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.GetAllCacheEntriesResult;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cacheclient.CacheClientFactory;
import com.bhf.aeroncache.services.cacheclient.MapCacheClientFactory;
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
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.io.File;
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
    private static final int DEFAULT_HTTP_PORT = 7070;
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

    public static void main(String[] args) {

        var cacheMode = System.getenv("CACHE_MODE");
        final boolean CLUSTERED_MODE = cacheMode == null || cacheMode.toUpperCase().equals("RAFT");

        System.out.println("Cache mode: " + cacheMode + ", using clustered mode: " + CLUSTERED_MODE);
        startHTTPInterface(DEFAULT_HTTP_PORT, CLUSTERED_MODE);
    }

    public static int startHTTPInterface(int port, boolean useClusteredMode) {

        System.out.println("Starting HTTP interface");
        tracingServiceName = System.getenv("OTEL_SERVICE_NAME");

        var app = startHTTPServer(port);

        try {
            final ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);
            System.out.println("Starting AeronCache Cluster Interface");

            CacheClientFactory clientFactory = new MapCacheClientFactory();
            var cacheRequestEncoder = clientFactory.getCacheRequestEncoder();
            var responseDecoder = clientFactory.getCacheResponseDecoder();
            var schemaDetailsProvider = clientFactory.getSchemaDetails();

            if (PRE_ENCODE_CACHE_REQUESTS) {
                // We encode the SBE messages before dropping them onto an Agrona RB for
                // sending directly to the cluster
                CacheRequestPublisher cacheRequestPublisher = new RBClusterMessagePublisher(cache, rb,
                        HttpIdleStrategies.clusterMessagePublisherIdleStrategy.get(), cacheRequestEncoder);

                BlockingClusterRequestPublisher blockingRequestPublisher = new ClusterMessagePublisher(cache,
                        HttpIdleStrategies.blockingPublisherIdleStrategy.get(), new RegularStringCacheRequestEncoder());
                observingPublisher = new ObservingClusterRequestPublisher(cacheRequestPublisher,
                        blockingRequestPublisher);
            } else {
                // Drop normalised cache requests onto an Agrona RB for encoding
                // to SBE on the Agent thread
                CacheRequestPublisher rbPublisher = new RBCacheRequestPublisher(rb);
                observingPublisher = new ObservingCacheRequestPublisher(rbPublisher);
            }

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider);
            client.setCacheResultsCallbacks(observingPublisher);

            var podName = System.getenv("POD_ADDRESS");
            var allHosts = System.getenv("CLUSTER_ADDRESSES");

            System.out.println("POD_ADDRESS=" + podName);
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);

            var egressIP = DNSUtils.getThisHostName();
            var hostArray = List.of(allHosts.split(","));
            var ingressEndpoints = ClusterUtils.ingressEndpoints(hostArray);

            System.out.println("Awaiting DNS Resolution");
            for (int i = 0; i < hostArray.size(); i++) {
                DNSUtils.awaitDnsResolution(hostArray, i);
            }

            System.out.println("DNS Resolution Complete. Building cluster connection now.");
            mediaDriver = ClusterUtils.launchEmbeddedMediaDriver();

            if (useClusteredMode) {
                buildClusterConnection(egressIP, ingressEndpoints);
            } else {
                final Aeron.Context aeronCtx = new Aeron.Context()
                        .aeronDirectoryName(mediaDriver.aeronDirectoryName());
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

    private static void buildClusterConnection(String egressIP, String ingressEndpoints) {
        try {
            aeronCluster = ClusterUtils.buildClusterConnection(egressIP, ingressEndpoints, client, "HTTPClient",
                    mediaDriver);
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
            buildClusterConnection(egressIP, ingressEndpoints);
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
                .get(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleGetItemRequest)
                .get(API_PREFIX + "<cacheId>", HttpApplication::handleGetCacheRequest)
                .post(API_PREFIX + "<cacheId>", HttpApplication::handlePutItemRequest)
                .delete(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleDeleteItemRequest)
                .delete(API_PREFIX + "<cacheId>", HttpApplication::handleDeleteCacheRequest)
                .patch(API_PREFIX + "<cacheId>", HttpApplication::handleClearCacheRequest)
                .get("/api/v1/caches", HttpApplication::handleGetCachesRequest)
                .get("/api/v1/stats", HttpApplication::handleGetStatsRequest)
                .get(LIVENESS, HttpApplication::handleGetLiveness)
                .get(READINESS, HttpApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .post("baselinePost", HttpApplication::postActionBaseline)
                .get("baselineGet", HttpApplication::getActionBaseline)
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
            CompletableFuture.runAsync(() -> observingPublisher.getAllCacheStats(requestId, c -> {
                log.info("Got cache stats, requestId {}", c.getRequestId());
                allCaches.clear();
                int totalOps = statsTracker.getTotalOpsCount().get();
                int totalCaches = 0;
                int totalItems = 0;

                var stats = c.getStats();
                for (var x : stats) {
                    totalCaches++;
                    totalItems += x.size;
                    allCaches.add(x.getCacheId().value());
                    cacheToSize.put(x.getCacheId().value(), x.size);
                }

                var statsTrackerStats = statsTracker.getCacheStats();
                var response = new CacheStats(totalOps, totalCaches, totalItems, statsTrackerStats.errorCount());
                future.complete(response);
            }));

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
            CompletableFuture.runAsync(() -> observingPublisher.deleteCache(requestId, cacheId, c -> {
                var deletedCacheId = c.getCacheId();
                log.info("Got delete cache response from cluster on cacheId {}", deletedCacheId);
                var response = new DeleteCacheResponse(deletedCacheId.value(), c.getStatus());
                future.complete(response);
            }));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                allCaches.remove(response.cacheId());
                statsTracker.getTotalCaches().decrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to delete cache with Id: \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
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
            CompletableFuture.runAsync(() -> observingPublisher.removeCacheEntry(requestId, cacheId, key, c -> {
                log.info("Got delete item response from cluster on cacheId {}, key {}", c.getCacheId(), c.getKey());
                var response = new DeleteItemResponse(c.getCacheId().value(), c.getKey().value(), c.getStatus());
                future.complete(response);
            }));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                statsTracker.getTotalItems().decrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg =
                    STR."Badly formed request to delete item with key \{ctx.pathParam("key")} from cache with Id: \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
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
            CompletableFuture.runAsync(() -> observingPublisher.clearCache(requestId, cacheId, c -> {
                log.info("Got clear cache response from cluster on cacheId {}", c.getCacheId());
                var response = new ClearCacheResponse(cacheId, c.getStatus());
                future.complete(response);
            }));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to clear cache with ID \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
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
            CompletableFuture.runAsync(() -> observingPublisher.getCacheEntry(requestId, cacheId, key, c -> {
                log.info("Get item response from cluster on cacheId {}, key {}, value {}", c.getCacheId(),
                        c.getEntryKey(), c.getEntryValue());
                var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
                var response = noCache ?
                        new GetItemResponse("0", "NA", "NA", c.getStatus()) :
                        new GetItemResponse(c.getCacheId().value(), c.getEntryKey().value(),
                                c.getEntryValue().value(), c.getStatus());
                future.complete(response);
            }));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg =
                    STR."Badly formed request to get item with key \{ctx.pathParam("key")} from cache with Id: \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES,
                    CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Handle a request to add an item to a cache.
     *
     * @param ctx The context.
     */
    private static void handlePutItemRequest(Context ctx) {
        try {
            var request = ctx.bodyAsClass(PutItemRequest.class);
            log.info("Got put item request on cacheId {}, key {}, value {}",
                    request.cacheId(), request.key(), request.value());

            var requestId = getRequestId(ctx);
            CompletableFuture<PutItemResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.addCacheEntry(requestId, request.cacheId(),
                    request.key(), request.value(), c -> {
                        var cacheId = c.getCacheId();
                        log.info("Got put item response from cluster on cacheId {}", cacheId);
                        var response = new PutItemResponse(cacheId.value(), request.key(), c.getStatus());
                        future.complete(response);
                    }));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                statsTracker.getTotalItems().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to put item from request: \{ctx.body()}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
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
            CompletableFuture.runAsync(() -> observingPublisher.sendCreateCache(requestId, request.cacheId(), c -> {
                var cacheId = c.getCacheId();
                log.info("Got create cache response from cluster on cacheId {}", cacheId);
                var response = new CreateCacheResponse(cacheId.value(), c.getStatus());
                future.complete(response);
            }));

            var response = future.get();

            if (response.operationStatus() == CacheOperationStatus.SUCCESS) {
                allCaches.add(response.cacheId());
                statsTracker.getTotalCaches().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to create cache from request: \{ctx.body()}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
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
            CompletableFuture.runAsync(() -> observingPublisher.getCacheEntries(requestId, cacheId, c -> {
                log.info("Get cache content response from cluster on cacheId {}", c.getCacheId());
                var noCache = c.getStatus() == CacheOperationStatus.UNKNOWN_CACHE;
                var response = noCache ?
                        new GetCacheResponse("0", CacheOperationStatus.UNKNOWN_CACHE, List.of()) :
                        new GetCacheResponse("0", c.getStatus(), buildItemsList(c));
                future.complete(response);
            }));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to get cache content for cache ID \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, CacheOperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
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
        return STR."\{currentTraceId}@\{currentSpanId}";
    }

}
