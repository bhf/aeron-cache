package com.bhf.aeroncache.integration.soak.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
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
    /**
     * When non-null, {@link #patchCount(String)} reads the synthetic-patch counter from the Prometheus
     * exposition text this supplies instead of scraping {@code baseUrl}'s own {@code /prometheus}. Used for
     * the Aeron gateway, whose near counters are Aeron driver counters absent from its Micrometer scrape but
     * served by a co-located clustertool reader of the gateway media driver's CnC file.
     */
    private final Supplier<String> patchMetricsSource;
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
        this.patchMetricsSource = null;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /**
     * Variant for an edge whose synthetic-patch counter is not carried on its own {@code /prometheus} but is
     * readable from an external Prometheus exposition source co-located with it. This is how the Aeron gateway
     * soak asserts the counter: near-cache management (prime/list/stop) still uses {@code baseUrl} (the
     * gateway's HTTP port), but {@link #patchCount(String)} parses the text from {@code patchMetricsSource} -
     * the clustertool's scrape of the gateway media driver's CnC file ({@code AeronCountersReader} +
     * {@code AeronCountersPrometheusFormatter}, the same code the clustertool sidecar runs in production). With
     * this constructor {@link #supportsPatchMetrics()} reports {@code true}, so the workload asserts per-outcome
     * counts rather than only verifying the synthetic stream events.
     *
     * @param baseUrl            the edge's HTTP origin for near-cache management, e.g. {@code http://localhost:8123}.
     * @param patchesMetricName  the synthetic-patch counter name as it appears in the external scrape
     *                           ({@code aeron_gateway_near_patches_total} for the Aeron gateway).
     * @param patchMetricsSource supplies the Prometheus exposition text carrying that counter; queried afresh
     *                           on each read so a before/after delta around a round reflects live increments.
     */
    public HttpNearCacheAdmin(String baseUrl, String patchesMetricName, Supplier<String> patchMetricsSource) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.patchesMetricName = patchesMetricName;
        this.patchMetricsExposed = true;
        this.patchMetricsSource = Objects.requireNonNull(patchMetricsSource, "patchMetricsSource");
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
        final String body;
        if (patchMetricsSource != null) {
            body = patchMetricsSource.get();
        } else {
            var resp = send("GET", PROMETHEUS_ENDPOINT);
            if (resp.statusCode() != 200) {
                throw new IllegalStateException("scrape /prometheus expected HTTP 200 but got " + resp.statusCode());
            }
            body = resp.body();
        }
        return sumCounter(body, outcome);
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
