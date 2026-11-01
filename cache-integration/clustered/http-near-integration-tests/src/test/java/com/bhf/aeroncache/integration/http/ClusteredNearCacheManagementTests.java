package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.NearCacheTestEndpoints;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

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
        var uri = getHttpUri(backend);
        var port = getHttpPort(backend);

        // Reading the cache (served transparently from the near cache on the regular read URI) subscribes
        // to it and requests a snapshot; the local copy is populated asynchronously as the snapshot arrives.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().get(endpoints.getItemEndpoint() + WARM_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // The cache should eventually show up in the warm list once the snapshot has populated it.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(WARM_CACHE_ID))
                        .body("find { it.cacheId == '" + WARM_CACHE_ID + "' }.itemCount", Matchers.equalTo(2)));

        // Stop near caching the cache: subscription dropped and local copy removed.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + WARM_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(WARM_CACHE_ID))
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // It should no longer be listed as warm.
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
        var uri = getHttpUri(backend);
        var port = getHttpPort(backend);

        // Prime the near cache up front, without any read or subscription: reports it was not already warm.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + PRIMED_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(PRIMED_CACHE_ID))
                .body("wasAlreadyWarm", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // The snapshot populates the local copy asynchronously; it is then listed warm with both items.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(NEAR_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(PRIMED_CACHE_ID))
                        .body("find { it.cacheId == '" + PRIMED_CACHE_ID + "' }.itemCount", Matchers.equalTo(2)));

        // Priming an already-warm cache is an idempotent no-op that reports it was already warm.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().post(NEAR_CACHE_PREFIX + PRIMED_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("wasAlreadyWarm", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // Stop near caching the cache: subscription dropped and local copy removed.
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
        RestAssured.given().port(getHttpPort(backend)).baseUri(getHttpUri(backend))
                .accept(ContentType.JSON)
                .when().delete(NEAR_CACHE_PREFIX + NEVER_WARMED_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(NEVER_WARMED_CACHE_ID))
                .body("wasCached", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }

    String getHttpUri(BackendTestResource backend) {
        return backend.getBaseHttpNearUri();
    }

    int getHttpPort(BackendTestResource backend) {
        return backend.getHttpNearPort();
    }
}
