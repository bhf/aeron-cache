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
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.HTTPStatusUtils;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import lombok.extern.log4j.Log4j2;
import org.agrona.ErrorHandler;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.agrona.concurrent.status.AtomicCounter;
import org.jetbrains.annotations.NotNull;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
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
    private static final int PORT_BASE = 9000;
    private static final int PORTS_PER_NODE = 100;
    static final int CLIENT_FACING_PORT_OFFSET = 2;
    private static AeronCacheListener client;
    private static ObservingClusterRequestPublisher observingPublisher;
    private static AeronCluster cluster;
    private static AtomicBoolean clusterConnected = new AtomicBoolean(false);

    public static void main(String[] args) {
        System.out.println("Starting HTTP interface");
        var app = startHTTPServer();

        try {
            ManyToOneRingBuffer rb = buildRingbuffer();
            System.out.println("Starting AeronCache Cluster Interface");
            observingPublisher = new ObservingClusterRequestPublisher(new AgentRequestPublisher(rb));
            client = new AeronCacheListener();
            client.setCacheResultsCallbacks(observingPublisher);

            var podName = System.getenv("POD_ADDRESS");
            var allHosts = System.getenv("CLUSTER_ADDRESSES");

            System.out.println("POD_ADDRESS=" + podName);
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);

            var egressIP = getThisHostName();
            var hostArray = List.of(allHosts.split(","));
            var ingressEndpoints = ingressEndpoints(hostArray);

            System.out.println("Awaiting DNS Resolution");
            for (int i = 0; i < hostArray.size(); i++) {
                DNSUtils.awaitDnsResolution(hostArray, i);
            }

            System.out.println("DNS Resolution Complete. Building cluster connection now.");
            cluster = buildClusterConnection(egressIP, ingressEndpoints);

            System.out.println("Building cluster agent");
            var idleStrategy = new BackoffIdleStrategy();
            ClusterClientAgent agent = new ClusterClientAgent(cluster, rb, idleStrategy);
            var errorHandler = getAgentRunnerErrorHandler();
            var errorCounter = getAgentErrorCounter();
            AgentRunner runner = new AgentRunner(new YieldingIdleStrategy(), errorHandler, errorCounter, agent);
            clusterConnected.set(true);
            AgentRunner.startOnThread(runner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ManyToOneRingBuffer buildRingbuffer() {
        var bufferSize = 4096 + RingBufferDescriptor.TRAILER_LENGTH;
        AtomicBuffer buffer = new UnsafeBuffer(new ExpandableDirectByteBuffer(bufferSize));
        return new ManyToOneRingBuffer(buffer);
    }

    private static AtomicCounter getAgentErrorCounter() {
        return cluster.context().aeron().addCounter(1, "AeronCacheAgent");
    }

    @NotNull
    private static ErrorHandler getAgentRunnerErrorHandler() {
        return cluster.context().errorHandler();
    }

    /**
     * Ingress endpoints generated from a list of hostnames.
     *
     * @param hostnames for the cluster members.
     * @return a formatted string of ingress endpoints for connecting to a cluster.
     */
    public static String ingressEndpoints(final List<String> hostnames) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnames.size(); i++) {
            sb.append(i).append('=');
            sb.append(hostnames.get(i)).append(':').append(
                    calculatePort(i, CLIENT_FACING_PORT_OFFSET));
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

    static int calculatePort(final int nodeId, final int offset) {
        return PORT_BASE + (nodeId * PORTS_PER_NODE) + offset;
    }

    /**
     * Start up a HTTP server for REST requests.
     *
     * @return The wired up Javalin instance.
     */
    private static Javalin startHTTPServer() {

        return Javalin.create(getHTTPConfig())
                .post(API_PREFIX, HttpApplication::handleCreateCacheRequest)
                .post(API_PREFIX + "<cacheId>", HttpApplication::handlePutItemRequest)
                .delete(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleDeleteItemRequest)
                .delete(API_PREFIX + "<cacheId>", HttpApplication::handleDeleteCacheRequest)
                .get(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleGetItemRequest)
                .get(LIVENESS, HttpApplication::handleGetLiveness)
                .get(READINESS, HttpApplication::handleGetReadiness)
                .start(PORT);
    }

    private static Consumer<JavalinConfig> getHTTPConfig() {
        return config -> config.bundledPlugins.enableCors(cors -> {
            cors.addRule(it -> {
                it.allowHost("http://localhost");
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
                var response = new DeleteCacheResponse(deletedCacheId.value());
                future.complete(response);
            }));

            ctx.json(future.get());
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to delete cache with Id: \{ ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.USE_NUMERIC_CACH_ID);
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
                log.info("Got delete on item from cluster on cacheId {}, key {}", c.getCacheId(), c.getKey());
                var response = new DeleteItemResponse(c.getCacheId().value(), c.getKey().value());
                future.complete(response);
            }));

            ctx.json(future.get());
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to delete item with key \{ctx.pathParam("key")} from cache with Id: \{ ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.USE_NUMERIC_CACH_ID);
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
                log.info("Got item from cluster on cacheId {}, key {}, value {}", c.getCacheId(), c.getEntryKey(), c.getEntryValue());
                var noCache = c.getStatus()== OperationStatus.UNKNOWN_CACHE;
                var response = noCache ?
                        new GetItemResponse(0, "NA", "NA") :
                        new GetItemResponse(c.getCacheId().value(), c.getEntryKey().value(), c.getEntryValue().value());
                future.complete(response);
            }));

            ctx.json(future.get());
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to get item with key \{ctx.pathParam("key")} from cache with Id: \{ ctx.pathParam("cacheId")}";
            log.warn(errorMsg);
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.USE_NUMERIC_CACH_ID);
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
                var response = new PutItemResponse(cacheId.getValue(), request.key());
                future.complete(response);
            }));

            ctx.json(future.get());
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to put item from request: \{ ctx.body()}";
            log.warn(errorMsg);
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES);
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
                var response = new CreateCacheResponse(cacheId.getValue());
                future.complete(response);
            }));

            ctx.json(future.get());
        } catch (Exception e) {
            var errorMsg = STR."Badly formed request to create cache from request: \{ ctx.body()}";
            log.warn(errorMsg);
            var badRequest = new RequestErrorResponse(errorMsg, ErrorMessages.CHECK_ALL_VALUES);
            ctx.status(HTTPStatusUtils.BAD_REQUEST);
            ctx.json(badRequest);
        }
    }

    /**
     * Build the connection to the cluster.
     *
     * @return An {@link AeronCluster} instance.
     */
    private static AeronCluster buildClusterConnection(String egressIP, String ingressEndpoints) {
        System.out.println("Building cluster connection...");
        MediaDriver mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
        return AeronCluster.connect(
                new AeronCluster.Context()
                        .egressListener(client)
                        .egressChannel("aeron:udp?endpoint=" + egressIP + ":0")
                        .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                        .ingressChannel("aeron:udp")
                        .ingressEndpoints(ingressEndpoints));
    }

    public static String getThisHostName() {
        try {
            final Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (interfaceEnumeration.hasMoreElements()) {
                final var networkInterface = interfaceEnumeration.nextElement();

                if (networkInterface.getName().startsWith("eth0")) {
                    System.out.println("Found eth0 interface: " + networkInterface);
                    final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
                    while (interfaceAddresses.hasMoreElements()) {
                        if (interfaceAddresses.nextElement() instanceof Inet4Address inet4Address) {
                            var address = inet4Address.getHostAddress();
                            System.out.println("Returning IP4 address: " + address);
                            return address;
                        }
                    }
                }
            }
        } catch (final Exception e) {
            // ignore
        }
        return "localhost";
    }
}
