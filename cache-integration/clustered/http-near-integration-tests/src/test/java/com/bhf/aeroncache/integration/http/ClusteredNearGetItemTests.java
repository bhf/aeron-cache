package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.NearCacheTestEndpoints;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@BackendTestConfig(httpEnabled = true, httpNearCacheEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredNearGetItemTests extends GetItemTests<String> {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String UNKNOWN_CACHE_ID = "123";
    private static final String UNKNOWN_KEY = "UNKNOWN_KEY";

    private final NearCacheTestEndpoints endpoints = new NearCacheTestEndpoints();

    public ClusteredNearGetItemTests() {
        super(new NearCacheTestEndpoints());
    }

    @Override
    String getKnownValue() {
        return "SomeValue";
    }

    @Override
    String getNotFoundValue() {
        return "";
    }

    @Override
    String getUnknownCacheValue() {
        return "NA";
    }

    /**
     * With near caching enabled the near endpoint does not read through to the source cluster: the first read
     * of a not-yet-warm key is a miss that kicks off priming, and the value is served only once the warming
     * subscription has populated the local copy. This replaces the base read-through expectation (a single GET
     * returning the value).
     */
    @Test
    @DisplayName("Should get an existing value back once the near cache is warm")
    @HappyPath
    @Override
    void shouldGetExistingCacheValue(BackendTestResource backend) {
        var port = getHttpPort(backend);
        var uri = getHttpUri(backend);
        var itemUri = endpoints.getItemEndpoint() + KNOWN_CACHE_ID + "/" + KNOWN_KEY;

        // Act: first read of the cold cache is a near-cache miss that starts priming (no cluster read-through).
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().get(itemUri)

                // Assert: a miss, not the value.
                .then().assertThat()
                .statusCode(404)
                .body("operationStatus", Matchers.comparesEqualTo("UNKNOWN_KEY"));

        // Assert: once the warming subscription has populated the local copy, the key is served locally.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(itemUri)
                        .then().assertThat()
                        .statusCode(200)
                        .body("value", Matchers.equalTo(getKnownValue())));
    }

    /**
     * An unknown cache is just a near-cache miss (the near cache never reads through to discover whether the
     * cluster knows the cache), so it returns a 404 {@code UNKNOWN_KEY} rather than the read-through
     * unknown-cache sentinel the base test asserts.
     */
    @Test
    @DisplayName("Should 404 as a near-cache miss on an unknown cache")
    @Override
    void shouldGet404OnUnknownCache(BackendTestResource backend) {
        RestAssured.given().port(getHttpPort(backend)).baseUri(getHttpUri(backend))
                .accept(ContentType.JSON)

                // Act
                .when().get(endpoints.getItemEndpoint() + UNKNOWN_CACHE_ID + "/" + UNKNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("operationStatus", Matchers.comparesEqualTo("UNKNOWN_KEY"));
    }

    @Override
    String getHttpUri(BackendTestResource backend) {
        return backend.getBaseHttpNearUri();
    }

    @Override
    int getHttpPort(BackendTestResource backend) {
        return backend.getHttpNearPort();
    }
}
