package com.bhf.aeroncache.application;

import com.bhf.aeroncache.models.CreateCacheRequest;
import com.bhf.aeroncache.models.CreateCacheResponse;
import com.bhf.aeroncache.services.cluster.ClusterClient;
import io.aeron.cluster.client.AeronCluster;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class HttpApplication {

    private static final int PORT = 7070;
    private static final String API_PREFIX = "/api/v1/cache/";

    public static void main(String[] args) {
        var client = new ClusterClient();
        AeronCluster cluster = buildClusterConnection();
        var app = startHTTPServer(client, cluster);
    }

    /**
     * Start up a HTTP server for REST requests.
     *
     * @param client The {@link ClusterClient} instance.
     * @param cluster The {@link AeronCluster} instance.
     *
     * @return The wired up Javalin instance.
     */
    private static Javalin startHTTPServer(ClusterClient client, AeronCluster cluster) {
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
        //client.sendCreateCacheSync(cluster, request.cacheId);
        var now = System.currentTimeMillis();
        CreateCacheResponse response = new CreateCacheResponse(now);
        ctx.json(response);
    }

    /**
     * Build the connection to the cluster.
     *
     * @return An {@link AeronCluster} instance.
     */
    private static AeronCluster buildClusterConnection() {
        return null;
    }
}
