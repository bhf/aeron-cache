package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
abstract class GetTimersTests<V> {

    private static final String TIMERS_ENDPOINT = "/api/v1/timers";
    private static final long TTL = 60000L;
    private static final String KNOWN_CACHE_ID = "getTimersCache";
    private static final String TIMER_KEY = "getTimersKey";
    private static TestEndpointsProvider endpointsProvider;

    GetTimersTests(TestEndpointsProvider endpoints) {
        endpointsProvider = endpoints;
    }

    abstract V getKnownValue();

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, endpointsProvider);
    }

    @Test
    @DisplayName("Should list a pending cache timer with its type, target and deadline")
    @HappyPath
    void shouldListPendingCacheTimer(BackendTestResource backend) {
        // Arrange - add an item with a ttl so a removal timer is scheduled
        CacheTestUtils.addItem(KNOWN_CACHE_ID, TIMER_KEY, getKnownValue(), TTL, backend, endpointsProvider);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .accept(ContentType.JSON)
                .when().get(TIMERS_ENDPOINT)

                // Assert - the scheduled removal is listed, tagged as a CACHE timer for our cache/key with a real deadline
                .then().assertThat()
                .statusCode(200)
                .body("operationStatus", Matchers.equalTo("SUCCESS"))
                .body("timers.find { it.cacheId == '" + KNOWN_CACHE_ID + "' && it.key == '" + TIMER_KEY + "' }.timerType",
                        Matchers.equalTo("CACHE"))
                .body("timers.find { it.cacheId == '" + KNOWN_CACHE_ID + "' && it.key == '" + TIMER_KEY + "' }.deadline",
                        Matchers.greaterThan(0L));
    }
}
