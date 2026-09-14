package com.bhf.aeroncache.integration.snapshot;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal HTTP helper (JDK client, no JUnit/RestAssured coupling) used by the snapshot harness to
 * seed and assert {@link SnapshotFixture} entries against a running cache HTTP interface.
 */
public final class SnapshotHttp {

    private static final String CACHE_API = "/api/v1/cache/";
    private static final String COUNTERS_API = "/api/v1/counters/";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String base;

    /** @param base e.g. {@code http://localhost:32784} */
    public SnapshotHttp(String base) {
        this.base = base;
    }

    /** Poll {@code /readiness} until it returns 200 or the timeout elapses. */
    public void awaitReady(Duration timeout) {
        var deadline = System.nanoTime() + timeout.toNanos();
        RuntimeException last = null;
        while (System.nanoTime() < deadline) {
            try {
                var res = send(HttpRequest.newBuilder(URI.create(base + "/readiness")).GET());
                if (res.statusCode() == 200) {
                    return;
                }
            } catch (RuntimeException e) {
                last = e;
            }
            sleep(2000);
        }
        throw new IllegalStateException("HTTP interface at " + base + " never became ready", last);
    }

    public void createCache(String cacheId) {
        var body = new JSONObject().put("cacheId", cacheId).toString();
        var res = send(post(base + CACHE_API, body));
        expect2xx("create cache " + cacheId, res);
    }

    public void putItem(String cacheId, String key, String value) {
        var body = new JSONObject().put("key", key).put("value", value).toString();
        var res = send(post(base + CACHE_API + cacheId, body));
        expect2xx("put " + cacheId + "/" + key, res);
    }

    /** @return the {@code value} field of the item, or {@code null} if the response has none. */
    public String getItemValue(String cacheId, String key) {
        var res = send(HttpRequest.newBuilder(URI.create(base + CACHE_API + cacheId + "/" + key)).GET());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("GET " + cacheId + "/" + key + " -> " + res.statusCode() + ": " + res.body());
        }
        var json = new JSONObject(res.body());
        return json.has("value") ? json.getString("value") : null;
    }

    public void createCounterCache(String cacheId) {
        var body = new JSONObject().put("cacheId", cacheId).toString();
        var res = send(post(base + COUNTERS_API, body));
        expect2xx("create counter cache " + cacheId, res);
    }

    public void putCounter(String cacheId, String key, long value) {
        var body = new JSONObject().put("key", key).put("value", value).toString();
        var res = send(post(base + COUNTERS_API + cacheId, body));
        expect2xx("put counter " + cacheId + "/" + key, res);
    }

    /** @return the numeric {@code value} of the counter, or {@code null} if the response has none. */
    public Long getCounterValue(String cacheId, String key) {
        var res = send(HttpRequest.newBuilder(URI.create(base + COUNTERS_API + cacheId + "/" + key)).GET());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("GET counter " + cacheId + "/" + key + " -> " + res.statusCode() + ": " + res.body());
        }
        var json = new JSONObject(res.body());
        return json.has("value") ? json.getLong("value") : null;
    }

    /** Seed every cache and entry of the fixture, including counter caches. */
    public void seed() {
        SnapshotFixture.CACHES.forEach(this::createCache);
        SnapshotFixture.ENTRIES.forEach(e -> putItem(e.cacheId(), e.key(), e.value()));
        SnapshotFixture.COUNTER_CACHES.forEach(this::createCounterCache);
        SnapshotFixture.COUNTER_ENTRIES.forEach(e -> putCounter(e.cacheId(), e.key(), e.value()));
    }

    /**
     * Trigger a cluster snapshot via the product endpoint. The endpoint's reported exit code is
     * unreliable (it returns -1 even when the snapshot is applied), so callers must confirm the
     * snapshot landed out of band (recording-log growth) rather than trust the response.
     */
    public void triggerSnapshot() {
        var res = send(HttpRequest.newBuilder(URI.create(base + "/api/v1/snapshot"))
                .POST(HttpRequest.BodyPublishers.noBody()));
        if (res.statusCode() / 100 != 2) {
            throw new IllegalStateException("snapshot request failed: " + res.statusCode() + " " + res.body());
        }
    }

    /**
     * Assert every fixture entry present at {@code artifactFixtureVersion} is readable with the
     * expected value. Throws {@link AssertionError} listing all mismatches.
     */
    public void verifyFixture(int artifactFixtureVersion) {
        var mismatches = new java.util.ArrayList<String>();

        for (var e : SnapshotFixture.entriesFor(artifactFixtureVersion)) {
            String actual;
            try {
                actual = getItemValue(e.cacheId(), e.key());
            } catch (RuntimeException ex) {
                mismatches.add(e.cacheId() + "/" + e.key() + " -> error: " + ex.getMessage());
                continue;
            }
            if (!e.value().equals(actual)) {
                mismatches.add(e.cacheId() + "/" + e.key() + " -> expected '" + preview(e.value())
                        + "' but got '" + preview(actual) + "'");
            }
        }

        for (var e : SnapshotFixture.counterEntriesFor(artifactFixtureVersion)) {
            Long actual;
            try {
                actual = getCounterValue(e.cacheId(), e.key());
            } catch (RuntimeException ex) {
                mismatches.add("counter " + e.cacheId() + "/" + e.key() + " -> error: " + ex.getMessage());
                continue;
            }
            if (actual == null || e.value() != actual) {
                mismatches.add("counter " + e.cacheId() + "/" + e.key() + " -> expected " + e.value()
                        + " but got " + actual);
            }
        }

        if (!mismatches.isEmpty()) {
            throw new AssertionError("Snapshot fixture mismatches:\n  " + String.join("\n  ", mismatches));
        }
    }

    private static String preview(String s) {
        if (s == null) {
            return "<null>";
        }
        return s.length() <= 32 ? s : s.substring(0, 32) + "...(" + s.length() + " chars)";
    }

    private HttpRequest.Builder post(String uri, String body) {
        return HttpRequest.newBuilder(URI.create(uri))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) {
        try {
            return client.send(builder.timeout(Duration.ofSeconds(15)).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private static void expect2xx(String what, HttpResponse<String> res) {
        if (res.statusCode() / 100 != 2) {
            throw new IllegalStateException(what + " failed: " + res.statusCode() + " " + res.body());
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
