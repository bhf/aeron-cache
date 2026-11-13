package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.events.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.metrics.PrometheusMetrics;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Shared integration tests for the streaming near cache: hydration served from the local near cache,
 * plus the management endpoints (list warm caches, stop near caching). Transport-specific subclasses
 * supply the streaming helper (WS or SSE) and where the management HTTP endpoints live for that
 * interface.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractStreamingNearCacheTests {

    private static final String NEAR_CACHES_ENDPOINT = "/api/v1/near/caches";
    private static final String NEAR_CACHE_PREFIX = "/api/v1/near/cache/";
    private static final long AWAIT_SECONDS = 60;

    private final TestEndpointsProvider httpEndpoints;
    private final StreamingHelper streamingHelper;

    /**
     * @param httpEndpoints   endpoints used to create caches and add items over HTTP.
     * @param streamingHelper the WS or SSE streaming client helper.
     */
    protected AbstractStreamingNearCacheTests(TestEndpointsProvider httpEndpoints, StreamingHelper streamingHelper) {
        this.httpEndpoints = httpEndpoints;
        this.streamingHelper = streamingHelper;
    }

    protected abstract String getCacheId();

    /** The base URI ({@code http://host}) of the streaming interface's management endpoints. */
    protected abstract String managementBaseUri(BackendTestResource backend);

    /** The port of the streaming interface's management endpoints. */
    protected abstract int managementPort(BackendTestResource backend);

    @Test
    @DisplayName("Should warm the near cache on a hydrating subscription, hydrate a second subscriber locally, list it, then drop it on stop")
    @HappyPath
    void shouldWarmHydrateListAndStop(BackendTestResource backend) {
        // Arrange: a seeded source cache, and a baseline of the near cache hit counter.
        var cacheId = getCacheId();
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2", backend, httpEndpoints);
        int hitsBefore = nearHydrationsServed(backend, "whole");

        // Act: the first hydrating subscriber finds the cache cold and is hydrated from the cluster (warming it).
        assertHydrates(backend, cacheId, Map.of("Key1", "Value1", "Key2", "Value2"));

        // Assert: a cold subscriber is served by the cluster, not the near cache, so the hit counter is
        // unchanged; and once the snapshot has populated it, the cache is listed warm with its item count.
        MatcherAssert.assertThat(nearHydrationsServed(backend, "whole"), Matchers.is(hitsBefore));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // Act: a second hydrating subscriber arrives while the cache is warm.
        assertHydrates(backend, cacheId, Map.of("Key1", "Value1", "Key2", "Value2"));

        // Assert: it was served from the near cache, so the hit counter incremented by exactly one - proving
        // this subscriber was hydrated locally rather than from the cluster.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                MatcherAssert.assertThat(nearHydrationsServed(backend, "whole"), Matchers.is(hitsBefore + 1)));

        // Act: stop near caching the cache.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId))
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // Assert: the local copy is dropped, so it is no longer listed as warm.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.not(Matchers.hasItem(cacheId))));
    }

    @Test
    @DisplayName("Should prime a cold near cache up front so a later hydrating subscriber is served locally")
    @HappyPath
    void shouldPrimeWarmHydrateAndStop(BackendTestResource backend) {
        // Arrange: seed the source cache with items over HTTP.
        var cacheId = getCacheId() + "-primed";
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2", backend, httpEndpoints);

        // Act: prime the near cache up front, with no subscription yet.
        // Assert: priming reports it was not already warm.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId))
                .body("wasAlreadyWarm", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // Assert: the snapshot populates the local copy asynchronously, after which it is listed warm.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // Act: prime the already-warm cache again.
        // Assert: it is an idempotent no-op that reports the cache was already warm.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasAlreadyWarm", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // Act: a hydrating subscriber arrives after priming, with the cache already warm.
        int hitsBefore = nearHydrationsServed(backend, "whole");
        assertHydrates(backend, cacheId, Map.of("Key1", "Value1", "Key2", "Value2"));

        // Assert: it was served the full state from the warm local copy, so the near cache hit counter
        // incremented by one (a cluster-served subscribe would have left it unchanged).
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                MatcherAssert.assertThat(nearHydrationsServed(backend, "whole"), Matchers.is(hitsBefore + 1)));

        // Act: stop near caching the cache, dropping the local copy.
        // Assert: the stop reports the cache was cached.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }

    @Test
    @DisplayName("Should serve a key-based hydrating subscription from the near cache when the cache is already primed")
    @HappyPath
    void shouldServeKeyBasedSubscriptionFromPrimedNearCache(BackendTestResource backend) {
        // Arrange: seed the source cache with two items over HTTP, then prime the near cache so the whole
        // cache is warm before any subscription arrives.
        var cacheId = getCacheId() + "-keyed";
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2", backend, httpEndpoints);
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasAlreadyWarm", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // Act: a key-based hydrating subscription for a single key arrives while the cache is warm.
        int hitsBefore = nearHydrationsServed(backend, "keyed");
        assertHydratesKeys(backend, cacheId, List.of("Key1"), Map.of("Key1", "Value1"));

        // Assert: it is served from the warm local copy, filtered to just that key (Key1 only, asserted by
        // assertHydratesKeys), and the keyed near cache hit counter incremented by one - proving local serving.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                MatcherAssert.assertThat(nearHydrationsServed(backend, "keyed"), Matchers.is(hitsBefore + 1)));

        // Clean up so repeated runs in this class instance start from a cold cache.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should serve a patch-mode subscription from the near cache as a full baseline then merge-patch deltas when the cache is primed")
    @HappyPath
    void shouldServePatchSubscriptionFromPrimedNearCache(BackendTestResource backend) {
        // Arrange: seed a JSON value and prime the cache so it is warm before the patch subscription arrives.
        var cacheId = getCacheId() + "-patch";
        var initial = "{\"a\":1,\"b\":2}";
        var updated = "{\"a\":1,\"b\":3}";
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", initial, backend, httpEndpoints);
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(1)));

        // Act: open a patch-mode hydrating subscription (served from the warm copy), and once it is
        // established, overwrite the value with a new full value.
        int deltasBefore = nearPatches(backend, "delta");
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getPatchEventsWithHydration(backend, cacheId, 2, ready);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(ready::isDone);
        CacheTestUtils.addItem(cacheId, "Key1", updated, backend, httpEndpoints);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        // Assert: the subscription receives a full ADD_ITEM baseline, then a near-cache-generated PATCH_ITEM
        // delta carrying only the changed field for the subsequent full overwrite.
        var baseline = events.stream()
                .filter(e -> e.eventType() == CacheUpdateEvent.EventType.ADD_ITEM)
                .findFirst().orElseThrow(() -> new AssertionError("expected a full ADD_ITEM baseline, got " + events));
        MatcherAssert.assertThat(baseline.itemKey(), Matchers.is("Key1"));
        MatcherAssert.assertThat(baseline.itemValue(), Matchers.is(initial));

        var patch = events.stream()
                .filter(e -> e.eventType() == CacheUpdateEvent.EventType.PATCH_ITEM)
                .findFirst().orElseThrow(() -> new AssertionError("expected a PATCH_ITEM delta, got " + events));
        MatcherAssert.assertThat(patch.itemKey(), Matchers.is("Key1"));
        MatcherAssert.assertThat(patch.itemValue(), Matchers.is("{\"b\":3}"));

        // Assert: the synthetic patch counter recorded exactly the one merge-patch delta we generated for
        // this subscriber, proving the patch was produced locally from the warm copy.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                MatcherAssert.assertThat(nearPatches(backend, "delta"), Matchers.is(deltasBefore + 1)));

        // Clean up so repeated runs in this class instance start from a cold cache.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should serve a key-filtered patch-mode subscription from the near cache - baseline then deltas for the subscribed key only")
    @HappyPath
    void shouldServeKeyedPatchSubscriptionFromPrimedNearCache(BackendTestResource backend) {
        // Arrange: seed two JSON keys and prime the cache so the whole cache is warm before the subscription
        // arrives. Key2 exists so that a broken key filter would leak it into either the hydration baseline or
        // the live delta stream.
        var cacheId = getCacheId() + "-keyed-patch";
        var key1Initial = "{\"a\":1,\"b\":2}";
        var key1Updated = "{\"a\":1,\"b\":3}";
        var key2Initial = "{\"x\":1}";
        var key2Updated = "{\"x\":2}";
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", key1Initial, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", key2Initial, backend, httpEndpoints);
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // Act: open a patch-mode subscription filtered to Key1 only (served from the warm copy). Once it is
        // established, overwrite Key2 first (which the filter must drop) and then Key1 (which must produce the
        // one delta that completes the subscription). The subscriber collects exactly two events: the Key1
        // baseline and the Key1 delta.
        int deltasBefore = nearPatches(backend, "delta");
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getPatchEventsForKeysWithHydration(backend, cacheId, List.of("Key1"), 2, ready);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(ready::isDone);
        CacheTestUtils.addItem(cacheId, "Key2", key2Updated, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", key1Updated, backend, httpEndpoints);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        // Assert: every delivered event is for Key1 - the filter excluded Key2 from both the hydration baseline
        // and the live patch stream even though Key2 was warm and was updated.
        MatcherAssert.assertThat("key-filtered patch subscription should only receive Key1, got " + events,
                events.stream().allMatch(e -> "Key1".equals(e.itemKey())), Matchers.is(true));

        // Assert: Key1 arrives as a full ADD_ITEM baseline, then a single near-cache-generated PATCH_ITEM delta
        // carrying only the changed field.
        var baseline = events.stream()
                .filter(e -> e.eventType() == CacheUpdateEvent.EventType.ADD_ITEM)
                .findFirst().orElseThrow(() -> new AssertionError("expected a full ADD_ITEM baseline, got " + events));
        MatcherAssert.assertThat(baseline.itemKey(), Matchers.is("Key1"));
        MatcherAssert.assertThat(baseline.itemValue(), Matchers.is(key1Initial));

        var patches = events.stream()
                .filter(e -> e.eventType() == CacheUpdateEvent.EventType.PATCH_ITEM)
                .collect(Collectors.toList());
        MatcherAssert.assertThat(patches, Matchers.hasSize(1));
        MatcherAssert.assertThat(patches.get(0).itemKey(), Matchers.is("Key1"));
        MatcherAssert.assertThat(patches.get(0).itemValue(), Matchers.is("{\"b\":3}"));

        // Assert: exactly one merge-patch delta was generated for this subscriber - the Key2 overwrite was
        // filtered out before patch generation, so it produced no delta.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                MatcherAssert.assertThat(nearPatches(backend, "delta"), Matchers.is(deltasBefore + 1)));

        // Clean up so repeated runs in this class instance start from a cold cache.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should fall back to a full ADD_ITEM for a patch subscriber when a valid merge patch cannot be generated")
    @HappyPath
    void shouldFallBackToFullValueWhenPatchGenerationFails(BackendTestResource backend) {
        // Arrange: seed a valid JSON value (so the warm copy has a parseable baseline to diff against) and
        // prime the cache so it is warm before the patch subscription arrives.
        var cacheId = getCacheId() + "-patch-fallback";
        var initial = "{\"a\":1,\"b\":2}";
        var corrupt = "not valid json";
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", initial, backend, httpEndpoints);
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(1)));

        // Act: open a patch-mode hydrating subscription (served from the warm copy), then overwrite the value
        // with one that is not parseable JSON, so no valid merge-patch delta can be computed from the warm copy.
        int fallbacksBefore = nearPatches(backend, "full_fallback");
        int deltasBefore = nearPatches(backend, "delta");
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getPatchEventsWithHydration(backend, cacheId, 2, ready);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(ready::isDone);
        CacheTestUtils.addItem(cacheId, "Key1", corrupt, backend, httpEndpoints);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        // Assert: the subscription receives the full ADD_ITEM baseline, then - because the patch could not be
        // built - the whole new value as another full ADD_ITEM rather than a PATCH_ITEM delta (which would have
        // left the subscriber stale) and never a PATCH_ITEM.
        MatcherAssert.assertThat("expected no PATCH_ITEM when patch generation fails, got " + events,
                events.stream().anyMatch(e -> e.eventType() == CacheUpdateEvent.EventType.PATCH_ITEM),
                Matchers.is(false));

        var adds = events.stream()
                .filter(e -> e.eventType() == CacheUpdateEvent.EventType.ADD_ITEM)
                .collect(Collectors.toList());
        MatcherAssert.assertThat(adds, Matchers.hasSize(2));
        MatcherAssert.assertThat(adds.get(0).itemKey(), Matchers.is("Key1"));
        MatcherAssert.assertThat(adds.get(0).itemValue(), Matchers.is(initial));
        MatcherAssert.assertThat(adds.get(1).itemKey(), Matchers.is("Key1"));
        MatcherAssert.assertThat(adds.get(1).itemValue(), Matchers.is(corrupt));

        // Assert: the fallback was recorded as a full_fallback (not a delta), proving the whole value was sent
        // because the patch could not be generated rather than as an ordinary delta or baseline.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                MatcherAssert.assertThat(nearPatches(backend, "full_fallback"), Matchers.is(fallbacksBefore + 1)));
        MatcherAssert.assertThat(nearPatches(backend, "delta"), Matchers.is(deltasBefore));

        // Clean up so repeated runs in this class instance start from a cold cache.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("Near warm caches gauge on /prometheus should match the warm cache listing and track stop")
    @HappyPath
    void shouldExposeWarmCacheCountAsNearWarmCachesMetric(BackendTestResource backend) {
        // Arrange: a seeded source cache warmed into the near cache by a hydrating subscription, confirmed
        // present on the authoritative warm cache listing.
        var cacheId = getCacheId() + "-metric";
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        assertHydrates(backend, cacheId, Map.of("Key1", "Value1"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat().statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId)));

        // Act: read the near warm caches gauge and the warm cache listing while the cache is warm.
        double warmGauge = PrometheusMetrics.sampleValue(managementBaseUri(backend), managementPort(backend),
                "aeroncache_near_warm_caches", Map.of("domain", "cache"));
        int warmListed = warmCacheCount(backend);

        // Assert: the gauge counts at least our cache and agrees exactly with the warm cache listing (both
        // are backed by the same warm-set, so they must never disagree at a quiescent point).
        MatcherAssert.assertThat(warmListed, Matchers.greaterThanOrEqualTo(1));
        MatcherAssert.assertThat((int) warmGauge, Matchers.is(warmListed));

        // Arrange: stop near caching the cache and wait for it to leave the warm listing.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat().statusCode(200)
                .body("wasCached", Matchers.equalTo(true));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat().statusCode(200)
                        .body("cacheId", Matchers.not(Matchers.hasItem(cacheId))));

        // Act: re-read the gauge and the listing after the stop.
        double warmGaugeAfter = PrometheusMetrics.sampleValue(managementBaseUri(backend), managementPort(backend),
                "aeroncache_near_warm_caches", Map.of("domain", "cache"));
        int warmListedAfter = warmCacheCount(backend);

        // Assert: the gauge dropped in step with the listing, by exactly the one cache we stopped.
        MatcherAssert.assertThat((int) warmGaugeAfter, Matchers.is(warmListedAfter));
        MatcherAssert.assertThat(warmListedAfter, Matchers.is(warmListed - 1));
    }

    private int warmCacheCount(BackendTestResource backend) {
        return RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().get(NEAR_CACHES_ENDPOINT)
                .then().assertThat().statusCode(200)
                .extract().jsonPath().getList("cacheId").size();
    }

    /**
     * Read the near cache hit counter ({@code aeroncache_near_hydrations_served_total}) for the cache domain
     * and the given subscription type ({@code whole} / {@code keyed}) from {@code /prometheus}. The counter
     * increments only when a subscription is hydrated from the warm local copy, so a delta of one across a
     * subscribe proves that subscribe was served by the near cache rather than the cluster.
     */
    private int nearHydrationsServed(BackendTestResource backend, String type) {
        return (int) PrometheusMetrics.sampleValue(managementBaseUri(backend), managementPort(backend),
                "aeroncache_near_hydrations_served_total", Map.of("domain", "cache", "type", type));
    }

    /**
     * Read the synthetic patch counter ({@code aeroncache_near_patches_total}) for the cache domain and the
     * given outcome ({@code delta} / {@code baseline} / {@code suppressed} / {@code full_fallback}) from
     * {@code /prometheus}. The
     * counter increments once per subscriber per update on the local patch path, so a delta of one across a
     * single-subscriber update proves a patch of that outcome was generated from the warm copy.
     */
    private int nearPatches(BackendTestResource backend, String outcome) {
        return (int) PrometheusMetrics.sampleValue(managementBaseUri(backend), managementPort(backend),
                "aeroncache_near_patches_total", Map.of("domain", "cache", "outcome", outcome));
    }

    /**
     * Read the non-hydrating ride counter ({@code aeroncache_near_subscriptions_served_total}) for the cache
     * domain and the given subscription type ({@code whole} / {@code keyed}) from {@code /prometheus}. The
     * counter increments only when a non-hydrating subscription rode the warm near cache instead of opening a
     * cluster subscription, so a delta of one across a subscribe proves the cluster subscribe was avoided.
     */
    private int nearSubscriptionsServed(BackendTestResource backend, String type) {
        return (int) PrometheusMetrics.sampleValue(managementBaseUri(backend), managementPort(backend),
                "aeroncache_near_subscriptions_served_total", Map.of("domain", "cache", "type", type));
    }

    @Test
    @DisplayName("Should serve a non-hydrating whole-cache subscription from the warm near cache without a cluster subscribe")
    @HappyPath
    void shouldServeNonHydratingWholeCacheSubscriptionFromPrimedNearCache(BackendTestResource backend) {
        // Arrange: seed a source cache and prime it so the whole cache is warm before the subscription arrives.
        var cacheId = getCacheId() + "-nonhydrating-whole";
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat().statusCode(200)
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat().statusCode(200)
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(1)));
        int servedBefore = nearSubscriptionsServed(backend, "whole");

        // Act: open a non-hydrating whole-cache subscription (no snapshot requested) and, once it is
        // established, push a single live update.
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getEvents(backend, cacheId, 1, ready);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(ready::isDone);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2", backend, httpEndpoints);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        // Assert: the subscriber received only the live update - the warm copy's Key1 was NOT replayed (it did
        // not ask to hydrate) - and the ride counter incremented by one, proving it rode the warm near cache
        // rather than opening a cluster subscription.
        MatcherAssert.assertThat(events, Matchers.hasSize(1));
        MatcherAssert.assertThat(events.get(0).eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
        MatcherAssert.assertThat(events.get(0).itemKey(), Matchers.is("Key2"));
        MatcherAssert.assertThat(events.get(0).itemValue(), Matchers.is("Value2"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                MatcherAssert.assertThat(nearSubscriptionsServed(backend, "whole"), Matchers.is(servedBefore + 1)));

        // Clean up so repeated runs in this class instance start from a cold cache.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat().statusCode(200);
    }

    @Test
    @DisplayName("Should serve a non-hydrating key-filtered subscription from the warm near cache without a cluster subscribe")
    @HappyPath
    void shouldServeNonHydratingKeyedSubscriptionFromPrimedNearCache(BackendTestResource backend) {
        // Arrange: seed two keys and prime the cache so the whole cache is warm. Key2 exists so a broken key
        // filter would leak it into the live stream.
        var cacheId = getCacheId() + "-nonhydrating-keyed";
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2", backend, httpEndpoints);
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat().statusCode(200)
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat().statusCode(200)
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));
        int servedBefore = nearSubscriptionsServed(backend, "keyed");

        // Act: open a non-hydrating subscription filtered to Key1 only. Once established, update Key2 (which the
        // filter must drop) then Key1 (which must be delivered as the one live event).
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getEventsForKeys(backend, cacheId, List.of("Key1"), 1, ready);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(ready::isDone);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2-updated", backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1-updated", backend, httpEndpoints);
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        // Assert: only the Key1 live update was delivered (no Key1 baseline replay, no Key2 leak), and the keyed
        // ride counter incremented by one, proving it rode the warm near cache without a cluster subscribe.
        MatcherAssert.assertThat(events, Matchers.hasSize(1));
        MatcherAssert.assertThat(events.get(0).eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
        MatcherAssert.assertThat(events.get(0).itemKey(), Matchers.is("Key1"));
        MatcherAssert.assertThat(events.get(0).itemValue(), Matchers.is("Value1-updated"));
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                MatcherAssert.assertThat(nearSubscriptionsServed(backend, "keyed"), Matchers.is(servedBefore + 1)));

        // Clean up so repeated runs in this class instance start from a cold cache.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat().statusCode(200);
    }

    @Test
    @DisplayName("Should succeed with wasCached=false when stopping a cache that was never warm")
    void shouldReportNotCachedWhenStoppingUnwarmedCache(BackendTestResource backend) {
        // Arrange: a cache id that was never warmed.
        var cacheId = getCacheId() + "-never-warmed";

        // Act: stop near caching the never-warmed cache.
        // Assert: the operation succeeds and reports the cache was not cached.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasCached", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }

    /**
     * Open a hydrating streaming subscription and assert it receives the expected items as ADD_ITEM
     * events, whatever the hydration source (cluster snapshot on a cold cache, near cache once warm).
     */
    private void assertHydrates(BackendTestResource backend, String cacheId, Map<String, String> expected) {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getEventsWithHydration(backend, cacheId, expected.size(), ready);

        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        MatcherAssert.assertThat(events, Matchers.hasSize(expected.size()));
        for (var event : events) {
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(cacheId));
        }
        var eventMap = events.stream().collect(Collectors.toMap(CacheUpdateEvent::itemKey, e -> (String) e.itemValue()));
        MatcherAssert.assertThat(eventMap, Matchers.is(expected));
    }

    /**
     * Open a key-based hydrating streaming subscription and assert it is hydrated with exactly the expected
     * items as ADD_ITEM events - the requested keys and nothing else.
     */
    private void assertHydratesKeys(BackendTestResource backend, String cacheId, List<String> keys, Map<String, String> expected) {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getEventsForKeysWithHydration(backend, cacheId, keys, expected.size(), ready);

        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        MatcherAssert.assertThat(events, Matchers.hasSize(expected.size()));
        for (var event : events) {
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(cacheId));
        }
        var eventMap = events.stream().collect(Collectors.toMap(CacheUpdateEvent::itemKey, e -> (String) e.itemValue()));
        MatcherAssert.assertThat(eventMap, Matchers.is(expected));
    }
}
