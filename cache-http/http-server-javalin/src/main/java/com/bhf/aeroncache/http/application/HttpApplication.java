package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.http.requests.CreateCacheRequest;
import com.bhf.aeroncache.http.requests.PutItemRequest;
import com.bhf.aeroncache.http.responses.*;
import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.models.results.GetAllCacheEntriesResult;
import com.bhf.aeroncache.services.cluster.AeronCacheListener;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.AgentRequestPublisher;
import com.bhf.aeroncache.services.cluster.impl.ObservingClusterRequestPublisher;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import com.bhf.aeroncache.utils.RingBufferUtils;
import io.aeron.cluster.client.AeronCluster;
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
import io.opentelemetry.api.trace.Span;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Log4j2
public class HttpApplication {

    public static final String PROMO_MICROMETER_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final int PORT = 7070;
    private static final String API_PREFIX = "/api/v1/cache/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static AeronCacheListener client;
    private static ObservingClusterRequestPublisher observingPublisher;
    private static AeronCluster cluster;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    private static final CacheStatsTracker statsTracker = new CacheStatsTracker();

    private static String tracingServiceName;

    public static void main(String[] args) {
        System.out.println("Starting HTTP interface");
        tracingServiceName = System.getenv("OTEL_SERVICE_NAME");

        var app = startHTTPServer();

        try {
            ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);
            System.out.println("Starting AeronCache Cluster Interface");
            observingPublisher = new ObservingClusterRequestPublisher(new AgentRequestPublisher(rb));
            client = new AeronCacheListener();
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
        registry.config().commonTags("application", "aeron-cache-http");

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
                .start(PORT);
    }

    /**
     * Used to establish a performance baseline for POST requests.
     * @param ctx
     */
    private static void postActionBaseline(Context ctx) {
        statsTracker.getTotalOpsCount().incrementAndGet();
    }

    /**
     * Used to establish a performance baseline for GET requests.
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
            CompletableFuture.runAsync(() -> observingPublisher.getAllCacheStatsBlocking(cluster, c -> {
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
                    cacheToSize.put(x.getCacheId().getValue(), x.size);
                }

                var statsTrackerStats = statsTracker.getCacheStats();
                var response = new CacheStats(totalOps, totalCaches, totalItems, statsTrackerStats.errorCount());
                future.complete(response);
            }, requestId));

            var response = future.get();

            ctx.status(HTTPStatusUtils.OK);
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = "Badly formed request to get cache stats";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, OperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    final static HashSet<Long> allCaches = new HashSet<>();
    final static Map<Long, Long> cacheToSize = new ConcurrentHashMap<>();

    /**
     * Handle getting details of available caches. Currently only
     * implemented in memory on the HTTP side.
     *
     * @param context The context.
     */
    private static void handleGetCachesRequest(Context context) {
        log.info("Got request to get all cache details");
        List<CacheDetails> cacheDetails = new ArrayList<>();
        for (Long l : allCaches) {
            var itemCount = cacheToSize.getOrDefault(l,0L);
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
            CompletableFuture.runAsync(() -> observingPublisher.deleteCacheBlocking(cluster, Long.parseLong(cacheId), c -> {
                var deletedCacheId = c.getCacheId();
                log.info("Got delete cache response from cluster on cacheId {}", deletedCacheId);
                var response = new DeleteCacheResponse(deletedCacheId.value(), c.getStatus());
                future.complete(response);
            }, requestId));

            var response = future.get();

            if (response.operationStatus() == OperationStatus.SUCCESS) {
                allCaches.remove(response.cacheId());
                statsTracker.getTotalCaches().decrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to delete cache with Id: \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.USE_NUMERIC_CACH_ID, OperationStatus.ERROR);
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
            var cacheId = Long.parseLong(ctx.pathParam("cacheId"));
            var key = ctx.pathParam("key");
            log.info("Got delete item request on cacheId {}, key {}",
                    cacheId, key);

            var requestId = getRequestId(ctx);
            CompletableFuture<DeleteItemResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.removeCacheEntryBlocking(cluster, cacheId, key, c -> {
                log.info("Got delete item response from cluster on cacheId {}, key {}", c.getCacheId(), c.getKey());
                var response = new DeleteItemResponse(c.getCacheId().value(), c.getKey().value(), c.getStatus());
                future.complete(response);
            }, requestId));

            var response = future.get();

            if (response.operationStatus() == OperationStatus.SUCCESS) {
                statsTracker.getTotalItems().decrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to delete item with key \{ctx.pathParam("key")} from cache with Id: \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.USE_NUMERIC_CACH_ID, OperationStatus.ERROR);
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
            var cacheId = Long.parseLong(ctx.pathParam("cacheId"));
            log.info("Got clear request on cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<ClearCacheResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.clearCacheBlocking(cluster, cacheId, c -> {
                log.info("Got clear cache response from cluster on cacheId {}", c.getCacheId());
                var response = new ClearCacheResponse(cacheId, c.getStatus());
                future.complete(response);
            }, requestId));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to clear cache with ID \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, OperationStatus.ERROR);
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
            var cacheId = Long.parseLong(ctx.pathParam("cacheId"));
            var key = ctx.pathParam("key");
            log.info("Got get item request on cacheId {}, key {}",
                    cacheId, key);

            var requestId = getRequestId(ctx);
            CompletableFuture<GetItemResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.getCacheEntryBlocking(cluster, cacheId, key, c -> {
                log.info("Get item response from cluster on cacheId {}, key {}, value {}", c.getCacheId(), c.getEntryKey(), c.getEntryValue());
                var noCache = c.getStatus() == OperationStatus.UNKNOWN_CACHE;
                var response = noCache ?
                        new GetItemResponse(0, "NA", "NA", c.getStatus()) :
                        new GetItemResponse(c.getCacheId().value(), c.getEntryKey().value(), c.getEntryValue().value(), c.getStatus());
                future.complete(response);
            }, requestId));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to get item with key \{ctx.pathParam("key")} from cache with Id: \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.USE_NUMERIC_CACH_ID, OperationStatus.ERROR);
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
            CompletableFuture.runAsync(() -> observingPublisher.addCacheEntryBlocking(cluster, request.cacheId(), request.key(), request.value(), c -> {
                var cacheId = c.getCacheId();
                log.info("Got put item response from cluster on cacheId {}", cacheId);
                var response = new PutItemResponse(cacheId.getValue(), request.key(), c.getStatus());
                future.complete(response);
            }, requestId));

            var response = future.get();

            if (response.operationStatus() == OperationStatus.SUCCESS) {
                statsTracker.getTotalItems().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to put item from request: \{ctx.body()}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, OperationStatus.ERROR);
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
            var requestId = getRequestId(ctx);

            CompletableFuture<CreateCacheResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.sendCreateCacheBlocking(cluster, request.cacheId(), c -> {
                var cacheId = c.getCacheId();
                log.info("Got create cache response from cluster on cacheId {}", cacheId);
                var response = new CreateCacheResponse(cacheId.getValue(), c.getStatus());
                future.complete(response);
            }, requestId));

            var response = future.get();

            if (response.operationStatus() == OperationStatus.SUCCESS) {
                allCaches.add(response.cacheId());
                statsTracker.getTotalCaches().incrementAndGet();
            }

            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to create cache from request: \{ctx.body()}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, OperationStatus.ERROR);
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
            var cacheId = Long.parseLong(ctx.pathParam("cacheId"));
            log.info("Got get cache content request on cacheId {}", cacheId);

            var requestId = getRequestId(ctx);
            CompletableFuture<GetCacheResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.getCacheEntriesBlocking(cluster, cacheId, c -> {
                log.info("Get cache content response from cluster on cacheId {}", c.getCacheId());
                var noCache = c.getStatus() == OperationStatus.UNKNOWN_CACHE;
                var response = noCache ?
                        new GetCacheResponse(0, OperationStatus.UNKNOWN_CACHE, List.of()) :
                        new GetCacheResponse(0, c.getStatus(), buildItemsList(c));
                future.complete(response);
            }, requestId));

            var response = future.get();
            ctx.status(HTTPStatusUtils.getHTTPCode(response.operationStatus()));
            ctx.json(response);
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to get cache content for cache ID \{ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            statsTracker.getTotalErrors().incrementAndGet();
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES, OperationStatus.ERROR);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    private static List<CacheItem> buildItemsList(GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> c) {
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
