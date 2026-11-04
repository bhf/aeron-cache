package com.bhf.aeroncache.integration.soak.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;

/**
 * A {@link NearCacheAdmin} that talks to a gateway edge's HTTP management surface over the JDK
 * {@link HttpClient}. Deliberately dependency-free (no OkHttp/Jackson): the near-caches list and the
 * {@code /prometheus} scrape are parsed with small regexes, which is enough for the two fields the soak needs
 * (an item count and a counter value) and keeps {@code soak-common} free of an HTTP/JSON dependency it does
 * not otherwise carry.
 *
 * @see NearCacheAdmin
 */
public final class HttpNearCacheAdmin implements NearCacheAdmin {

    private static final Logger log = LogManager.getLogger(HttpNearCacheAdmin.class);

    private static final String NEAR_CACHES_ENDPOINT = "/api/v1/near/caches";
    private static final String NEAR_CACHE_PREFIX = "/api/v1/near/cache/";
    private static final String PROMETHEUS_ENDPOINT = "/prometheus";
    private static final long POLL_NANOS = 200_000_000L; // 200ms between warm-state polls.

    private final String baseUrl;
    private final String patchesMetricName;
    private final boolean patchMetricsExposed;
    private final HttpClient http;

    /**
     * @param baseUrl             the edge's HTTP origin, e.g. {@code http://localhost:8123} (no trailing slash).
     * @param patchesMetricName   the synthetic-patch counter name as it appears on {@code /prometheus} -
     *                            {@code aeroncache_near_patches_total} for the web edges (WS/SSE/bidi) or
     *                            {@code aeron_gateway_near_patches_total} for the Aeron gateway.
     * @param patchMetricsExposed whether that counter is actually carried on this edge's {@code /prometheus}
     *                            (true for the Micrometer web edges; false for the Aeron gateway, whose near
     *                            counters are Aeron driver counters its Micrometer {@code /prometheus} omits).
     */
    public HttpNearCacheAdmin(String baseUrl, String patchesMetricName, boolean patchMetricsExposed) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.patchesMetricName = patchesMetricName;
        this.patchMetricsExposed = patchMetricsExposed;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean supportsPatchMetrics() {
        return patchMetricsExposed;
    }

    @Override
    public void prime(String cacheId) {
        var resp = send("POST", NEAR_CACHE_PREFIX + cacheId);
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("prime " + cacheId + " expected HTTP 200 but got " + resp.statusCode()
                    + ": " + resp.body());
        }
    }

    @Override
    public void stopNear(String cacheId) {
        var resp = send("DELETE", NEAR_CACHE_PREFIX + cacheId);
        if (resp.statusCode() != 200) {
            // Best-effort cleanup: a non-200 here should not mask the round's real assertions.
            log.warn("stopNear {} returned HTTP {}: {}", cacheId, resp.statusCode(), resp.body());
        }
    }

    @Override
    public void awaitWarm(String cacheId, int expectedItemCount, long timeoutNanos) {
        var deadline = System.nanoTime() + timeoutNanos;
        int lastSeen = -1;
        while (true) {
            var resp = send("GET", NEAR_CACHES_ENDPOINT);
            if (resp.statusCode() == 200) {
                lastSeen = itemCountFor(resp.body(), cacheId);
                if (lastSeen == expectedItemCount) {
                    return;
                }
            }
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("timed out awaiting near cache " + cacheId + " to warm to "
                        + expectedItemCount + " items (last observed itemCount=" + lastSeen + ")");
            }
            LockSupport.parkNanos(POLL_NANOS);
        }
    }

    @Override
    public long patchCount(String outcome) {
        var resp = send("GET", PROMETHEUS_ENDPOINT);
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("scrape /prometheus expected HTTP 200 but got " + resp.statusCode());
        }
        return sumCounter(resp.body(), outcome);
    }

    // ---------------------------------------------------------------- parsing

    /**
     * Find the {@code itemCount} of the object whose {@code cacheId} matches, scanning each {@code {...}} object
     * in the near-caches JSON array independently so field order within an object does not matter. Returns -1
     * when the cache is not listed yet.
     */
    private static int itemCountFor(String json, String cacheId) {
        var object = Pattern.compile("\\{[^}]*}").matcher(json);
        var idPattern = Pattern.compile("\"cacheId\"\\s*:\\s*\"" + Pattern.quote(cacheId) + "\"");
        var countPattern = Pattern.compile("\"itemCount\"\\s*:\\s*(\\d+)");
        while (object.find()) {
            var obj = object.group();
            if (idPattern.matcher(obj).find()) {
                var count = countPattern.matcher(obj);
                if (count.find()) {
                    return Integer.parseInt(count.group(1));
                }
            }
        }
        return -1;
    }

    /**
     * Sum every sample of {@link #patchesMetricName} whose label set contains {@code domain="cache"} and the
     * given {@code outcome}. The Aeron gateway also carries a {@code node} label, so several matching lines are
     * summed; the web edge has a single line per outcome.
     */
    private long sumCounter(String scrape, String outcome) {
        // e.g. aeroncache_near_patches_total{application="x",domain="cache",outcome="delta",} 3.0
        var line = Pattern.compile("^" + Pattern.quote(patchesMetricName) + "\\{([^}]*)}\\s+([0-9.eE+-]+)\\s*$",
                Pattern.MULTILINE);
        var m = line.matcher(scrape);
        double total = 0;
        boolean any = false;
        while (m.find()) {
            var labels = m.group(1);
            if (labels.contains("domain=\"cache\"") && labels.contains("outcome=\"" + outcome + "\"")) {
                total += Double.parseDouble(m.group(2));
                any = true;
            }
        }
        // An outcome counter that has never been incremented is simply absent from the scrape -> count 0.
        return any ? Math.round(total) : 0L;
    }

    // ---------------------------------------------------------------- http

    private HttpResponse<String> send(String method, String path) {
        try {
            var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException(method + " " + path + " failed: " + e.getMessage(), e);
        }
    }
}
