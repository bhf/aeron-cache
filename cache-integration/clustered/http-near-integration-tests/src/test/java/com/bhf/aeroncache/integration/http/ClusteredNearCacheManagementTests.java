package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.NearCacheTestEndpoints;
import com.bhf.aeroncache.integration.metrics.PrometheusMetrics;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for the near cache management endpoints: listing the warm caches
 * ({@code GET /api/v1/near/caches}), priming a cache up front
 * ({@code POST /api/v1/near/cache/<cacheId>}) and stopping near caching for a cache
 * ({@code DELETE /api/v1/near/cache/<cacheId>}).
 */
@BackendTestConfig(httpEnabled = true, httpNearCacheEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
class ClusteredNearCacheManagementTests {

    private static final String NEAR_CACHE_PREFIX = "/api/v1/near/cache/";
    private static final String NEAR_CACHES_ENDPOINT = "/api/v1/near/caches";
    private static final String WARM_CACHE_ID = "NearMgmtWarmCacheId";
    private static final String PRIMED_CACHE_ID = "NearMgmtPrimedCacheId";
    private static final String NEVER_WARMED_CACHE_ID = "NearMgmtNeverWarmedCacheId";

    private final NearCacheTestEndpoints endpoints = new NearCacheTestEndpoints();

    @BeforeAll
    void setup(BackendTestResource backend) {
        // Create the source caches (on the cluster, via the regular endpoints) and seed them with items so
        // the subscription snapshot has something to hydrate the local near copy with.
        CacheTestUtils.createCache(WARM_CACHE_ID, backend, endpoints);
        CacheTestUtils.addItem(WARM_CACHE_ID, "Key1", "Value1", backend, endpoints);
        CacheTestUtils.addItem(WARM_CACHE_ID, "Key2", "Value2", backend, endpoints);

        CacheTestUtils.createCache(PRIMED_CACHE_ID, backend, endpoints);
        CacheTestUtils.addItem(PRIMED_CACHE_ID, "Key1", "Value1", backend, endpoints);
        CacheTestUtils.addItem(PRIMED_CACHE_ID, "Key2", "Value2", backend, endpoints);
    }

    @Test
    @DisplayName("Should list a cache as warm after reading it, then drop it on stop")
    @HappyPath
    void shouldWarmListAndStopNearCache(BackendTestResource backend) {
        // Arrange: the seeded source cache (created in setup) and the interface address.
        var uri = getHttpUri(backend);
        var port = getHttpPort(backend);

        // Act: read the cache, served transparently from the near cache on the regular read URI, which
        // subscribes to it and requests a snapshot; the local copy is populated asynchronously.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().get(endpoints.getItemEndpoint() + WARM_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // Assert: once the snapshot has populated it, the cache shows up in the warm list with its items.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(WARM_CACHE_ID))
                        .body("find { it.cacheId == '" + WARM_CACHE_ID + "' }.itemCount", Matchers.equalTo(2)));

        // Act: stop near caching the cache, dropping the subscription and the local copy.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + WARM_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(WARM_CACHE_ID))
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // Assert: it is no longer listed as warm.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.not(Matchers.hasItem(WARM_CACHE_ID))));
    }

    @Test
    @DisplayName("Should prime a cache up front (without reading it) so it is listed warm, then drop it on stop")
    @HappyPath
    void shouldPrimeWarmAndStopNearCache(BackendTestResource backend) {
        // Arrange: the seeded source cache (created in setup) and the interface address.
        var uri = getHttpUri(backend);
        var port = getHttpPort(backend);

        // Act: prime the near cache up front, without any read or subscription.
        // Assert: priming reports it was not already warm.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + PRIMED_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(PRIMED_CACHE_ID))
                .body("wasAlreadyWarm", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // Assert: the snapshot populates the local copy asynchronously, after which it is listed warm.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(PRIMED_CACHE_ID))
                        .body("find { it.cacheId == '" + PRIMED_CACHE_ID + "' }.itemCount", Matchers.equalTo(2)));

        // Act: prime the already-warm cache again.
        // Assert: it is an idempotent no-op that reports the cache was already warm.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + PRIMED_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("wasAlreadyWarm", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // Act: stop near caching the cache, dropping the subscription and the local copy.
        // Assert: the stop reports the cache was cached.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + PRIMED_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }

    @Test
    @DisplayName("Should succeed with wasCached=false when stopping a cache that was never warm")
    void shouldReportNotCachedWhenStoppingUnwarmedCache(BackendTestResource backend) {
        // Arrange: a cache id that was never warmed, and the interface address.
        var uri = getHttpUri(backend);
        var port = getHttpPort(backend);

        // Act: stop near caching the never-warmed cache.
        // Assert: the operation succeeds and reports the cache was not cached.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + NEVER_WARMED_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(NEVER_WARMED_CACHE_ID))
                .body("wasCached", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }

    @Test
    @DisplayName("Near warm caches gauge on /prometheus should match the warm cache listing and track stop")
    @HappyPath
    void shouldExposeWarmCacheCountAsNearWarmCachesMetric(BackendTestResource backend) {
        // Arrange: a freshly seeded source cache, warmed into the near cache by a transparent read, and
        // confirmed present on the authoritative warm cache listing.
        var uri = getHttpUri(backend);
        var port = getHttpPort(backend);
        var cacheId = "NearMetricWarmCacheId";
        CacheTestUtils.createCache(cacheId, backend, endpoints);
        CacheTestUtils.addItem(cacheId, "Key1", "Value1", backend, endpoints);
        RestAssured.given().port(port).baseUri(uri).accept(ContentType.JSON)
                .when().get(endpoints.getItemEndpoint() + cacheId)
                .then().assertThat().statusCode(200);
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri).accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat().statusCode(200)
                        .body("cacheId", Matchers.hasItem(cacheId)));

        // Act: read the near warm caches gauge and the warm cache listing while the cache is warm.
        double warmGauge = PrometheusMetrics.sampleValue(uri, port, "aeroncache_near_warm_caches", Map.of("domain", "cache"));
        int warmListed = warmCacheCount(uri, port);

        // Assert: the gauge counts at least our cache and agrees exactly with the warm cache listing (both
        // are backed by the same warm-set, so they must never disagree at a quiescent point).
        MatcherAssert.assertThat(warmListed, Matchers.greaterThanOrEqualTo(1));
        MatcherAssert.assertThat((int) warmGauge, Matchers.is(warmListed));

        // Arrange: stop near caching the cache and wait for it to leave the warm listing.
        RestAssured.given().port(port).baseUri(uri).accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + cacheId)
                .then().assertThat().statusCode(200)
                .body("wasCached", Matchers.equalTo(true));
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri).accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat().statusCode(200)
                        .body("cacheId", Matchers.not(Matchers.hasItem(cacheId))));

        // Act: re-read the gauge and the listing after the stop.
        double warmGaugeAfter = PrometheusMetrics.sampleValue(uri, port, "aeroncache_near_warm_caches", Map.of("domain", "cache"));
        int warmListedAfter = warmCacheCount(uri, port);

        // Assert: the gauge dropped in step with the listing, by exactly the one cache we stopped.
        MatcherAssert.assertThat((int) warmGaugeAfter, Matchers.is(warmListedAfter));
        MatcherAssert.assertThat(warmListedAfter, Matchers.is(warmListed - 1));
    }

    private int warmCacheCount(String uri, int port) {
        return RestAssured.given().port(port).baseUri(uri).accept(ContentType.JSON)
                .when().get(NEAR_CACHES_ENDPOINT)
                .then().assertThat().statusCode(200)
                .extract().jsonPath().getList("cacheId").size();
    }

    String getHttpUri(BackendTestResource backend) {
        return backend.getBaseHttpNearUri();
    }

    int getHttpPort(BackendTestResource backend) {
        return backend.getHttpNearPort();
    }
}
