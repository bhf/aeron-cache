package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
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
 * Integration tests for the near counter cache management endpoints: listing the warm counter caches
 * ({@code GET /api/v1/near/counters-caches}) and stopping near caching for a counter cache
 * ({@code DELETE /api/v1/near/counters/<cacheId>}).
 */
@BackendTestConfig(httpEnabled = true, httpNearCacheEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
class ClusteredNearCountersManagementTests {

    private static final String NEAR_COUNTERS_PREFIX = "/api/v1/near/counters/";
    private static final String NEAR_COUNTERS_CACHES_ENDPOINT = "/api/v1/near/counters-caches";
    private static final String WARM_CACHE_ID = "NearMgmtWarmCounterCacheId";
    private static final String NEVER_WARMED_CACHE_ID = "NearMgmtNeverWarmedCounterCacheId";

    private final CounterTestEndpoints endpoints = new CounterTestEndpoints();

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(WARM_CACHE_ID, backend, endpoints);
        CacheTestUtils.addItem(WARM_CACHE_ID, "Key1", 10L, backend, endpoints);
        CacheTestUtils.addItem(WARM_CACHE_ID, "Key2", 20L, backend, endpoints);
    }

    @Test
    @DisplayName("Should list a counter cache as warm after reading it, then drop it on stop")
    @HappyPath
    void shouldWarmListAndStopNearCounters(BackendTestResource backend) {
        var uri = backend.getBaseHttpNearUri();
        var port = backend.getHttpNearPort();

        // Reading the near counter cache subscribes to it and requests a snapshot; the local copy is
        // populated asynchronously as the snapshot arrives.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().get(NEAR_COUNTERS_PREFIX + WARM_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // The counter cache should eventually show up in the warm list once the snapshot has populated it.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(NEAR_COUNTERS_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.hasItem(WARM_CACHE_ID))
                        .body("find { it.cacheId == '" + WARM_CACHE_ID + "' }.itemCount", Matchers.equalTo(2)));

        // Stop near caching the counter cache: subscription dropped and local copy removed.
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().delete(NEAR_COUNTERS_PREFIX + WARM_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(WARM_CACHE_ID))
                .body("wasCached", Matchers.equalTo(true))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));

        // It should no longer be listed as warm.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(NEAR_COUNTERS_CACHES_ENDPOINT)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.not(Matchers.hasItem(WARM_CACHE_ID))));
    }

    @Test
    @DisplayName("Should succeed with wasCached=false when stopping a counter cache that was never warm")
    void shouldReportNotCachedWhenStoppingUnwarmedCounterCache(BackendTestResource backend) {
        RestAssured.given().port(backend.getHttpNearPort()).baseUri(backend.getBaseHttpNearUri())
                .accept(ContentType.JSON)
                .when().delete(NEAR_COUNTERS_PREFIX + NEVER_WARMED_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(NEVER_WARMED_CACHE_ID))
                .body("wasCached", Matchers.equalTo(false))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"));
    }
}
