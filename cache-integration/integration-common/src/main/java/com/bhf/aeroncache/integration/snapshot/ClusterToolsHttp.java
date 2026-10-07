package com.bhf.aeroncache.integration.snapshot;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal HTTP helper for the cluster tools sidecar's own endpoints (snapshot info + purge), as
 * opposed to {@link SnapshotHttp} which drives the cache HTTP interface. Constructed with the base
 * URL that reaches the sidecar directly (e.g. via the node's published 7080 port).
 */
public final class ClusterToolsHttp {

    private static final String CLUSTERTOOLS_API = "/api/v1/clustertools/";
    private static final String SNAPSHOT_INFO = CLUSTERTOOLS_API + "snapshot-info";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String base;

    /** @param base e.g. {@code http://localhost:32785} reaching the clustertools sidecar. */
    public ClusterToolsHttp(String base) {
        this.base = base;
    }

    /** Poll {@code /readiness/} until it returns 200 or the timeout elapses. */
    public void awaitReady(Duration timeout) {
        var deadline = System.nanoTime() + timeout.toNanos();
        RuntimeException last = null;
        while (System.nanoTime() < deadline) {
            try {
                var res = send(HttpRequest.newBuilder(URI.create(base + "/readiness/")).GET());
                if (res.statusCode() == 200) {
                    return;
                }
            } catch (RuntimeException e) {
                last = e;
            }
            sleep(2000);
        }
        throw new IllegalStateException("Cluster tools sidecar at " + base + " never became ready", last);
    }

    /** @return the parsed snapshot-info response. */
    public JSONObject snapshotInfo() {
        var res = send(HttpRequest.newBuilder(URI.create(base + SNAPSHOT_INFO)).GET());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("snapshot-info -> " + res.statusCode() + ": " + res.body());
        }
        return new JSONObject(res.body());
    }

    /**
     * Purge old log recording segments (no new snapshot).
     *
     * @return the parsed purge response (fields: success, reclaimedBytes, purgedToPosition, ...).
     */
    public JSONObject purge() {
        return postTool("purge");
    }

    /**
     * Take a snapshot, await durability, then purge.
     *
     * @return the parsed purge response.
     */
    public JSONObject snapshotAndPurge() {
        return postTool("snapshot-and-purge");
    }

    private JSONObject postTool(String tool) {
        var body = new JSONObject().put("tool", tool).put("clusterFolder", "").toString();
        var res = send(HttpRequest.newBuilder(URI.create(base + CLUSTERTOOLS_API))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
        // The handler returns 500 with a JSON body on an unsuccessful purge; surface the body either way.
        if (res.body() == null || res.body().isBlank()) {
            throw new IllegalStateException(tool + " -> " + res.statusCode() + " with empty body");
        }
        return new JSONObject(res.body());
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) {
        try {
            return client.send(builder.timeout(Duration.ofSeconds(120)).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
