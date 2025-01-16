package com.bhf.aeroncache.application;

import com.bhf.aeroncache.models.CreateCacheRequest;
import com.bhf.aeroncache.models.CreateCacheResponse;
import com.bhf.aeroncache.services.cluster.ClusterClient;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;

public class HttpApplication {

    private static final int PORT = 7070;
    private static final String API_PREFIX = "/api/v1/cache/";
    private static final int PORT_BASE = 9000;
    private static final int PORTS_PER_NODE = 100;
    static final int CLIENT_FACING_PORT_OFFSET = 2;

    private static ClusterClient client;
    private static AeronCluster cluster;

    public static void main(String[] args) {
        client = new ClusterClient();
        final String egressIP = "localhost";
        final var ingressEndpoints = ingressEndpoints(List.of("localhost", "localhost", "localhost"));
        cluster = buildClusterConnection(egressIP, ingressEndpoints);
        var app = startHTTPServer();
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
        return Javalin.create(/*config*/)
                .post(API_PREFIX, HttpApplication::handleCreateCacheRequest)
                .post(API_PREFIX + "<cacheId>", HttpApplication::handlePutItemRequest)
                .delete(API_PREFIX + "<cacheId>", HttpApplication::handleDeleteCacheRequest)
                .get(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleGetItemRequest)
                .delete(API_PREFIX + "<cacheId>/<key>", HttpApplication::handleDeleteItemRequest)
                .start(PORT);
    }

    /**
     * Handle a request to delete a cache.
     *
     * @param context The context.
     */
    private static void handleDeleteCacheRequest(Context context) {
    }

    /**
     * Handle a request to delete an item from a cache.
     *
     * @param context The context.
     */
    private static void handleDeleteItemRequest(Context context) {
    }

    /**
     * Handle a request to get an item from a cache.
     *
     * @param context The context.
     */
    private static void handleGetItemRequest(Context context) {
    }

    /**
     * Handle a request to add an item to a cache.
     *
     * @param context The context.
     */
    private static void handlePutItemRequest(Context context) {
    }

    /**
     * Handle a request to create a cache.
     *
     * @param ctx The context.
     */
    private static void handleCreateCacheRequest(Context ctx) {
        var request = ctx.bodyAsClass(CreateCacheRequest.class);
        client.sendCreateCacheSync(cluster, request.cacheId(), c -> {
            var cacheId = c.getCacheId();
            CreateCacheResponse response = new CreateCacheResponse(cacheId);
            ctx.json(response);
        });

    }

    /**
     * Build the connection to the cluster.
     *
     * @return An {@link AeronCluster} instance.
     */
    private static AeronCluster buildClusterConnection(String egressIP, String ingressEndpoints) {
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
}
