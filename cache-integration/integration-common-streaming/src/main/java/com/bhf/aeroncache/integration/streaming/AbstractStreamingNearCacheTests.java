package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
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
        var cacheId = getCacheId();

        // Seed the source cache with items over HTTP.
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2", backend, httpEndpoints);

        // The first hydrating subscriber warms the near cache (and is itself hydrated).
        assertHydrates(backend, cacheId, Map.of("Key1", "Value1", "Key2", "Value2"));

        // Once the near cache has been warmed, it is listed with its item count.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // A second hydrating subscriber is now served the full state from the local near cache.
        assertHydrates(backend, cacheId, Map.of("Key1", "Value1", "Key2", "Value2"));

        // Stop near caching the cache: the local copy is dropped.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId))
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // It should no longer be listed as warm.
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
        var cacheId = getCacheId() + "-primed";

        // Seed the source cache with items over HTTP.
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2", backend, httpEndpoints);

        // Prime the near cache up front, with no subscription yet: reports it was not already warm.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId))
                .body("wasAlreadyWarm", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // The snapshot populates the local copy asynchronously; it is then listed warm with both items.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // Priming an already-warm cache is an idempotent no-op that reports it was already warm.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasAlreadyWarm", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // A hydrating subscriber arriving after priming is served the full state from the warm local copy.
        assertHydrates(backend, cacheId, Map.of("Key1", "Value1", "Key2", "Value2"));

        // Stop near caching the cache: the local copy is dropped.
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
        var cacheId = getCacheId() + "-keyed";

        // Seed the source cache with two items over HTTP.
        CacheTestUtils.createCache(cacheId, backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, httpEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", "Value2", backend, httpEndpoints);

        // Prime the near cache up front so the whole cache is warm before any subscription arrives.
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

        // A key-based hydrating subscription for a single key is served from the warm local copy, filtered to
        // just that key: it receives Key1 only, not the whole cache.
        assertHydratesKeys(backend, cacheId, List.of("Key1"), Map.of("Key1", "Value1"));

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
        var cacheId = getCacheId() + "-patch";
        var initial = "{\"a\":1,\"b\":2}";
        var updated = "{\"a\":1,\"b\":3}";

        // Seed a JSON value and prime the cache so it is warm before the patch subscription arrives.
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

        // A patch-mode hydrating subscription is served from the warm copy: a full ADD_ITEM baseline, then a
        // near-cache-generated PATCH_ITEM delta for the subsequent full overwrite.
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getPatchEventsWithHydration(backend, cacheId, 2, ready);

        // Once the subscription is established, overwrite the value with a new full value.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(ready::isDone);
        CacheTestUtils.addItem(cacheId, "Key1", updated, backend, httpEndpoints);

        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        var baseline = events.stream()
                .filter(e -> e.eventType() == CacheUpdateEvent.EventType.ADD_ITEM)
                .findFirst().orElseThrow(() -> new AssertionError("expected a full ADD_ITEM baseline, got " + events));
        MatcherAssert.assertThat(baseline.itemKey(), Matchers.is("Key1"));
        MatcherAssert.assertThat(baseline.itemValue(), Matchers.is(initial));

        var patch = events.stream()
                .filter(e -> e.eventType() == CacheUpdateEvent.EventType.PATCH_ITEM)
                .findFirst().orElseThrow(() -> new AssertionError("expected a PATCH_ITEM delta, got " + events));
        MatcherAssert.assertThat(patch.itemKey(), Matchers.is("Key1"));
        // The delta carries only the changed field, not the whole value.
        MatcherAssert.assertThat(patch.itemValue(), Matchers.is("{\"b\":3}"));

        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should succeed with wasCached=false when stopping a cache that was never warm")
    void shouldReportNotCachedWhenStoppingUnwarmedCache(BackendTestResource backend) {
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + getCacheId() + "-never-warmed")
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
