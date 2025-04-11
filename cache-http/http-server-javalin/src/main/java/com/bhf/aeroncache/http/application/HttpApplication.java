package com.bhf.aeroncache.http.application;

import com.bhf.aeroncache.http.requests.CreateCacheRequest;
import com.bhf.aeroncache.http.requests.PutItemRequest;
import com.bhf.aeroncache.http.responses.*;
import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.ErrorMessages;
import com.bhf.aeroncache.services.cluster.AeronCacheListener;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.AgentRequestPublisher;
import com.bhf.aeroncache.services.cluster.impl.ObservingClusterRequestPublisher;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import com.bhf.aeroncache.utils.RingBufferUtils;
import io.aeron.cluster.client.AeronCluster;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Log4j2
public class HttpApplication {

    private static final int PORT = 7070;
    private static final String API_PREFIX = "/api/v1/cache/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static AeronCacheListener client;
    private static ObservingClusterRequestPublisher observingPublisher;
    private static AeronCluster cluster;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    private static final CacheStatsTracker statsTracker = new CacheStatsTracker();

    public static void main(String[] args) {
        System.out.println("Starting HTTP interface");
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

        return Javalin.create(getHTTPConfig())
                .before(API_PREFIX + "*", _ -> statsTracker.getTotalOpsCount().incrementAndGet())
                .post(API_PREFIX, HttpApplication::handleCreateCacheRequest)
                .post(API_PREFIX + "<cacheId>", HttpApplication::handlePutItemRequest)
                .delete(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleDeleteItemRequest)
                .delete(API_PREFIX + "<cacheId>", HttpApplication::handleDeleteCacheRequest)
                .get(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleGetItemRequest)
                .get("/api/v1/caches", HttpApplication::handleGetCachesRequest)
                .get("/api/v1/stats", HttpApplication::handleGetStatsRequest)
                .get(LIVENESS, HttpApplication::handleGetLiveness)
                .get(READINESS, HttpApplication::handleGetReadiness)
                .start(PORT);
    }

    private static void handleGetStatsRequest(Context context) {
        log.info("Got request to get cache stats");
        context.json(statsTracker.getCacheStats());
    }

    final static HashSet<Long> allCaches = new HashSet<>();

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
            cacheDetails.add(new CacheDetails(l, 0));
        }
        context.json(cacheDetails);
    }

    /**
     * Basic configuration for CORS.
     *
     * @return Config for Javalin.
     */
    private static Consumer<JavalinConfig> getHTTPConfig() {
        return config -> config.bundledPlugins.enableCors(cors -> {
            cors.addRule(it -> {
                it.allowHost("http://localhost:3000", "http://localhost");
            });
        });
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

            CompletableFuture<DeleteCacheResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.deleteCacheBlocking(cluster, Long.parseLong(cacheId), c -> {
                var deletedCacheId = c.getCacheId();
                log.info("Got delete cache response from cluster on cacheId {}", deletedCacheId);
                var response = new DeleteCacheResponse(deletedCacheId.value(), c.getStatus());
                future.complete(response);
            }));

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

            CompletableFuture<DeleteItemResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.removeCacheEntryBlocking(cluster, cacheId, key, c -> {
                log.info("Got delete item response from cluster on cacheId {}, key {}", c.getCacheId(), c.getKey());
                var response = new DeleteItemResponse(c.getCacheId().value(), c.getKey().value(), c.getStatus());
                future.complete(response);
            }));

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

            CompletableFuture<GetItemResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.getCacheEntryBlocking(cluster, cacheId, key, c -> {
                log.info("Get item response from cluster on cacheId {}, key {}, value {}", c.getCacheId(), c.getEntryKey(), c.getEntryValue());
                var noCache = c.getStatus() == OperationStatus.UNKNOWN_CACHE;
                var response = noCache ?
                        new GetItemResponse(0, "NA", "NA", c.getStatus()) :
                        new GetItemResponse(c.getCacheId().value(), c.getEntryKey().value(), c.getEntryValue().value(), c.getStatus());
                future.complete(response);
            }));

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

            CompletableFuture<PutItemResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.addCacheEntryBlocking(cluster, request.cacheId(), request.key(), request.value(), c -> {
                var cacheId = c.getCacheID();
                log.info("Got put item response from cluster on cacheId {}", cacheId);
                var response = new PutItemResponse(cacheId.getValue(), request.key(), c.getStatus());
                future.complete(response);
            }));

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

            CompletableFuture<CreateCacheResponse> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> observingPublisher.sendCreateCacheBlocking(cluster, request.cacheId(), c -> {
                var cacheId = c.getCacheId();
                log.info("Got create cache response from cluster on cacheId {}", cacheId);
                var response = new CreateCacheResponse(cacheId.getValue(), c.getStatus());
                future.complete(response);
            }));

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

}
