package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.config.HttpNearCacheIdleStrategies;
import com.bhf.aeroncache.http.requests.CreateCacheRequest;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.http.responses.CreateCacheResponse;
import com.bhf.aeroncache.http.responses.GetItemResponse;
import com.bhf.aeroncache.http.responses.RequestErrorResponse;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.GroupedResponseHandler;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cacheclient.CacheClientFactory;
import com.bhf.aeroncache.services.cluster.BlockingClusterRequestPublisher;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.services.cluster.impl.ObservingClusterRequestPublisher;
import com.bhf.aeroncache.services.cluster.impl.RBClusterMessagePublisher;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import com.bhf.aeroncache.utils.RingBufferUtils;
import com.bhf.aeroncache.ws.application.CacheSubscriptionRequestPublisher;
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
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Log4j2
public class NearCacheApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final int PORT = 7073;
    private static final String API_PREFIX = "/api/v1/near/cache/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static final boolean PRE_ENCODE_CACHE_REQUESTS = false;

    private static final ConcurrentNearCacheManager nearCacheManager = new ConcurrentNearCacheManager();
    private static final CacheStatsTracker statsTracker = new CacheStatsTracker();
    private static AeronCacheClusterListener client;
    private static ObservingCacheRequestPublisher observingPublisher;
    private static CacheSubscriptionRequestPublisher subscriptionService;
    private static AeronCache cache;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);

    private static String tracingServiceName;
    private static AgentRunner agentRunner;
    private static AeronCluster aeronCluster;
    private static MediaDriver mediaDriver;

    private static final Pattern specialCharacters = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!]");

    public static void main(String[] args) {
        System.out.println("Starting HTTP interface");
        tracingServiceName = System.getenv("OTEL_SERVICE_NAME");

        var app = startHTTPServer();

        try {
            final ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);
            System.out.println("Starting AeronCache Cluster Interface");

            CacheClientFactory clientFactory = getCacheClientFactory();
            var cacheRequestEncoder = clientFactory.getCacheRequestEncoder();
            var responseDecoder = clientFactory.getCacheResponseDecoder();
            var schemaDetailsProvider = clientFactory.getSchemaDetails();

            if (PRE_ENCODE_CACHE_REQUESTS) {
                // We encode the SBE messages before dropping them onto an Agrona RB for
                // sending directly to the cluster
                CacheRequestPublisher cacheRequestPublisher = new RBClusterMessagePublisher(cache, rb,
                        HttpNearCacheIdleStrategies.clusterMessagePublisherIdleStrategy.get(), cacheRequestEncoder);

                BlockingClusterRequestPublisher blockingRequestPublisher = new ClusterMessagePublisher(cache,
                        HttpNearCacheIdleStrategies.blockingPublisherIdleStrategy.get(), cacheRequestEncoder);
                observingPublisher = new ObservingClusterRequestPublisher(cacheRequestPublisher,
                        blockingRequestPublisher);

                subscriptionService = new CacheSubscriptionRequestPublisher(cacheRequestPublisher);
            } else {
                // Drop normalised cache requests onto an Agrona RB for encoding
                // to SBE on the Agent thread
                CacheRequestPublisher rbPublisher = new RBCacheRequestPublisher(rb);
                observingPublisher = new ObservingCacheRequestPublisher(rbPublisher);
                subscriptionService = new CacheSubscriptionRequestPublisher(rbPublisher);
            }

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider);
            client.setCacheResultsCallbacks(new GroupedResponseHandler(
                    List.of(observingPublisher,
                    subscriptionService)));

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

            var cacheMode = System.getenv("CACHE_MODE");
            final boolean CLUSTERED_MODE = cacheMode == null || cacheMode.equalsIgnoreCase("RAFT");

            System.out.println("Cache mode: " + cacheMode + ", using clustered mode: " + CLUSTERED_MODE);

            if (CLUSTERED_MODE) {
                buildClusterConnection(egressIP, ingressEndpoints);
            } else {
                final Aeron.Context aeronCtx = new Aeron.Context()
                        .aeronDirectoryName(mediaDriver.aeronDirectoryName());
                final Aeron aeron = Aeron.connect(aeronCtx);
                var requestPubHost = System.getenv("REQUEST_PUB_HOST");
                buildUnclusteredConnection(aeron, requestPubHost);
            }

            System.out.println("Building cluster agent for http near cache service");
            var clusterClientAgentIdleStrategy = HttpNearCacheIdleStrategies.clusterClientAgentIdleStrategy.get();
            var clusterMessagePublisherIdleStrategy = HttpNearCacheIdleStrategies.clusterMessagePublisherIdleStrategy.get();
            var agent = PRE_ENCODE_CACHE_REQUESTS ?
                    new ClusterClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-ClusterClient-Agent") :
                    new CacheClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-CacheClient-Agent");

            var errorHandler = aeronCluster != null ? ClusterUtils.getAgentRunnerErrorHandler(aeronCluster) :
                    new RethrowingErrorHandler();
            var errorCounter = aeronCluster != null ? ClusterUtils.getAgentErrorCounter(aeronCluster, "HTTPClient") :
                    null;
            final IdleStrategy agentRunnerIdleStrategy = HttpNearCacheIdleStrategies.agentRunnerIdleStrategy.get();
            agentRunner = new AgentRunner(agentRunnerIdleStrategy, errorHandler, errorCounter, agent);
            clusterConnected.set(true);
            AgentRunner.startOnThread(agentRunner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        var requestPublicationChannel = "aeron:udp?endpoint=" + requestPubHost + ":8008|alias=AC-nc-unclustered-requests";
        int requestPublicationStream = 1;
        var requestPublication = aeron.addPublication(requestPublicationChannel,
                requestPublicationStream);

        var hostname = DNSUtils.getThisHostName();
        var responseSubscriptionChannel = "aeron:udp?endpoint=" + hostname + ":8007|alias=AC-nc-unclustered-responses";
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

        IdleStrategy unclusteredAgentIdleStrategy = HttpNearCacheIdleStrategies.unclusteredAgentIdleStrategy.get();
        final AgentRunner serverAgentRunner = new AgentRunner(unclusteredAgentIdleStrategy,
                Throwable::printStackTrace,
                null, serverAgent);

        AgentRunner.startOnThread(serverAgentRunner);
    }

    private static void buildClusterConnection(String egressIP, String ingressEndpoints) {
        aeronCluster = ClusterUtils.buildClusterConnection(egressIP, ingressEndpoints, client, "HTTPNearClient",
                mediaDriver);
        //addClusterErrorHandler(aeronCluster);

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
    }

    private static void shutdown() {
        CloseHelper.close(agentRunner);
        CloseHelper.close(mediaDriver);
        CloseHelper.close(aeronCluster);
    }

    private static void addClusterErrorHandler(AeronCluster aeronCluster) {
        aeronCluster.context().errorHandler(throwable -> clusterConnected.set(false));
    }

    /**
     * Start up a HTTP server for REST requests.
     *
     * @return The wired up Javalin instance.
     */
    private static Javalin startHTTPServer() {

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
                .beforeMatched(NearCacheApplication::checkClusterConnectivity)
                .post(API_PREFIX, NearCacheApplication::handleCreateCacheRequest)
                .get(API_PREFIX + "<cacheId>/<key>", NearCacheApplication::handleGetItemRequest)
                .get(LIVENESS, NearCacheApplication::handleGetLiveness)
                .get(READINESS, NearCacheApplication::handleGetReadiness)
                .get("/prometheus", ctx -> ctx.contentType(PROMO_MICROMETER_CONTENT_TYPE).result(registry.scrape()))
                .start(PORT);
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

            // check if the item exists in the local cache, otherwise get it from the remote cache
            if(nearCacheManager.contains(cacheId)){
                var localCache = nearCacheManager.get(cacheId);
                handleExistingLocalCache(ctx, localCache, key, cacheId);
            }
            else{
                handleNewNearCacheRequest(ctx, cacheId, key);
            }
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
     * Get an item from the source cache and also subscribe to updates for the cache.
     *
     * @param ctx
     * @param cacheId
     * @param key
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static void handleNewNearCacheRequest(Context ctx, String cacheId, String key) throws InterruptedException, ExecutionException {
        CompletableFuture<GetItemResponse> future = getItemFromSourceCache(ctx, cacheId, key);
        var response = future.get();

        if(response.operationStatus() == CacheOperationStatus.SUCCESS){
            nearCacheManager.put(cacheId, key, response.value());
            subscribeToCache(cacheId);
        }

        ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
        ctx.json(response);
    }

    /**
     * Handle a request where the local near cache already exists.
     *
     * @param ctx
     * @param localCache
     * @param key
     * @param cacheId
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static void handleExistingLocalCache(Context ctx, NearCacheInstance localCache, String key, String cacheId) throws InterruptedException, ExecutionException {
        if(localCache.containsKey(key)){
            var result = localCache.get(key);
            ctx.status(HTTPStatusUtils.OK);
            ctx.json(new GetItemResponse(cacheId, key, result, CacheOperationStatus.SUCCESS));
        }
        else{
            returnItemFromSourceCache(ctx, key, cacheId);
        }
    }

    /**
     * Get an item from the source cache and update the local near cache.
     *
     * @param ctx
     * @param key
     * @param cacheId
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static void returnItemFromSourceCache(Context ctx, String key, String cacheId) throws InterruptedException, ExecutionException {
        CompletableFuture<GetItemResponse> future = getItemFromSourceCache(ctx, cacheId, key);
        var response = future.get();

        if(response.operationStatus() == CacheOperationStatus.SUCCESS){
            nearCacheManager.put(cacheId, key, response.value());
        }
        ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
        ctx.json(response);
    }

    private static CompletableFuture<GetItemResponse> getItemFromSourceCache(Context ctx, String cacheId, String key) {
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
        return future;
    }

    /**
     * Handle a request to create a cache.
     *
     * @param ctx The context.
     */
    private static void handleCreateCacheRequest(Context ctx) {
        try {
            var request = ctx.bodyAsClass(CreateCacheRequest.class);
            log.info("Got create near cache request on cacheId {}", request.cacheId());

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

            if (response.operationStatus() == CacheOperationStatus.SUCCESS ||
                    response.operationStatus() == CacheOperationStatus.CACHE_EXISTS) {
                subscribeToCache(request.cacheId());
            }

            if(response.operationStatus() == CacheOperationStatus.SUCCESS){
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
     * Subscribe to the remote cache and update the local copy.
     *
     * @param cacheId The cache Id to subscribe to.
     */
    private static void subscribeToCache(String cacheId) {
        Consumer<Void> subscriptionFailureHandler = _ -> log.warn("Subscription to cacheId {} failed", cacheId);
        String sessionId = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString();
        Consumer<CacheUpdateEvent> resultsConsumer = cacheUpdateEvent -> {
            log.debug("Received cache update event for cacheId {}, key {}, value {}",
                    cacheUpdateEvent.cacheId(),
                    cacheUpdateEvent.itemKey(),
                    cacheUpdateEvent.itemValue());

            nearCacheManager.handleCacheUpdate(cacheUpdateEvent);
        };
        subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, cacheId, sessionId, requestId, resultsConsumer);
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
