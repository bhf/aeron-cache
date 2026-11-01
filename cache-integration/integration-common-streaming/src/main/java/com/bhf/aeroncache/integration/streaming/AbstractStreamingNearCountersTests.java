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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Shared integration tests for the streaming near counter cache: hydration served from the local near
 * cache, plus the counter management endpoints. The counter equivalent of
 * {@link AbstractStreamingNearCacheTests} - counter hydration events carry numeric values.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractStreamingNearCountersTests {

    private static final String NEAR_COUNTERS_CACHES_ENDPOINT = "/api/v1/near/counters-caches";
    private static final String NEAR_COUNTERS_PREFIX = "/api/v1/near/counters/";
    private static final long AWAIT_SECONDS = 60;

    private final TestEndpointsProvider httpCounterEndpoints;
    private final StreamingHelper streamingHelper;

    /**
     * @param httpCounterEndpoints endpoints used to create counter caches and set counters over HTTP.
     * @param streamingHelper      the WS or SSE streaming client helper bound to the counter endpoints.
     */
    protected AbstractStreamingNearCountersTests(TestEndpointsProvider httpCounterEndpoints, StreamingHelper streamingHelper) {
        this.httpCounterEndpoints = httpCounterEndpoints;
        this.streamingHelper = streamingHelper;
    }

    protected abstract String getCacheId();

    /** The base URI ({@code http://host}) of the streaming interface's management endpoints. */
    protected abstract String managementBaseUri(BackendTestResource backend);

    /** The port of the streaming interface's management endpoints. */
    protected abstract int managementPort(BackendTestResource backend);

    @Test
    @DisplayName("Should warm the near counter cache on a hydrating subscription, hydrate a second subscriber locally, list it, then drop it on stop")
    @HappyPath
    void shouldWarmHydrateListAndStopCounters(BackendTestResource backend) {
        var cacheId = getCacheId();

        // Seed the source counter cache over HTTP.
        CacheTestUtils.createCache(cacheId, backend, httpCounterEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", 10L, backend, httpCounterEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", 20L, backend, httpCounterEndpoints);

        // The first hydrating subscriber warms the near counter cache (and is itself hydrated).
        assertHydrates(backend, cacheId, Map.of("Key1", 10L, "Key2", 20L));

        // Once warmed, it is listed with its item count.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_COUNTERS_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // A second hydrating subscriber is now served the full state from the local near counter cache.
        assertHydrates(backend, cacheId, Map.of("Key1", 10L, "Key2", 20L));

        // Stop near caching the counter cache.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_COUNTERS_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId))
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // It should no longer be listed as warm.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_COUNTERS_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.not(Matchers.hasItem(cacheId))));
    }

    @Test
    @DisplayName("Should prime a cold near counter cache up front so a later hydrating subscriber is served locally")
    @HappyPath
    void shouldPrimeWarmHydrateAndStopCounters(BackendTestResource backend) {
        var cacheId = getCacheId() + "-primed";

        // Seed the source counter cache over HTTP.
        CacheTestUtils.createCache(cacheId, backend, httpCounterEndpoints);
        CacheTestUtils.addItem(cacheId, "Key1", 10L, backend, httpCounterEndpoints);
        CacheTestUtils.addItem(cacheId, "Key2", 20L, backend, httpCounterEndpoints);

        // Prime the near counter cache up front, with no subscription yet: reports it was not already warm.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_COUNTERS_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId))
                .body("wasAlreadyWarm", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // The snapshot populates the local copy asynchronously; it is then listed warm with both items.
        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                        .accept(ContentType.JSON)
                        .when().get(NEAR_COUNTERS_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId))
                        .body("find { it.cacheId == '" + cacheId + "' }.itemCount", Matchers.equalTo(2)));

        // Priming an already-warm cache is an idempotent no-op that reports it was already warm.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().post(NEAR_COUNTERS_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasAlreadyWarm", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // A hydrating subscriber arriving after priming is served the full state from the warm local copy.
        assertHydrates(backend, cacheId, Map.of("Key1", 10L, "Key2", 20L));

        // Stop near caching the counter cache: the local copy is dropped.
        RestAssured.given().port(managementPort(backend)).baseUri(managementBaseUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_COUNTERS_PREFIX + cacheId)
                .then().assertThat()
                .statusCode(200)
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }

    /**
     * Open a hydrating streaming subscription and assert it receives the expected counters as ADD_ITEM
     * events. Counter values arrive as JSON numbers, so they are normalised to {@code long}.
     */
    private void assertHydrates(BackendTestResource backend, String cacheId, Map<String, Long> expected) {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        var eventsFuture = streamingHelper.getEventsWithHydration(backend, cacheId, expected.size(), ready);

        Awaitility.await().atMost(AWAIT_SECONDS, TimeUnit.SECONDS).until(eventsFuture::isDone);
        var events = eventsFuture.join();

        MatcherAssert.assertThat(events, Matchers.hasSize(expected.size()));
        for (var event : events) {
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(cacheId));
        }
        var eventMap = events.stream().collect(
                Collectors.toMap(CacheUpdateEvent::itemKey, e -> ((Number) e.itemValue()).longValue()));
        MatcherAssert.assertThat(eventMap, Matchers.is(expected));
    }
}
