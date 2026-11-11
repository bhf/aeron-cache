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

import java.util.Map;
import java.util.concurrent.TimeUnit;

@BackendTestConfig(httpEnabled = true, httpNearCacheEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredNearGetCacheTests extends GetCacheTests<String> {

    private static final String KNOWN_CACHE_ID = "GetCacheTestId";

    private final NearCacheTestEndpoints endpoints = new NearCacheTestEndpoints();

    public ClusteredNearGetCacheTests() {
        super(new NearCacheTestEndpoints());
    }

    @Override
    Map<String, String> getKnownItems() {
        return Map.of(
                "Key1", "Value1",
                "Key2", "Value2",
                "Key3", "Value3"
        );
    }

    /**
     * With near caching enabled the near endpoint does not read a cold cache through from the cluster: the
     * first whole-cache read is a miss that kicks off priming, and the contents are served only once the
     * warming subscription has populated the local copy. This replaces the base read-through expectation (a
     * single GET returning all items).
     */
    @Test
    @DisplayName("Should get all items once the near cache is warm")
    @HappyPath
    @Override
    void shouldGetAllCacheEntries(BackendTestResource backend) {
        var port = getHttpPort(backend);
        var uri = getHttpUri(backend);
        var cacheUri = endpoints.getItemEndpoint() + KNOWN_CACHE_ID;

        // Act: first whole-cache read of the cold cache is a miss that starts priming (no cluster read-through).
        RestAssured.given().port(port).baseUri(uri)
                .accept(ContentType.JSON)
                .when().get(cacheUri)

                // Assert: a miss, not the contents.
                .then().assertThat()
                .statusCode(404)
                .body("operationStatus", Matchers.comparesEqualTo("UNKNOWN_CACHE"));

        // Assert: once the warming subscription has populated the local copy, the whole cache is served locally.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                RestAssured.given().port(port).baseUri(uri)
                        .accept(ContentType.JSON)
                        .when().get(cacheUri)
                        .then().assertThat()
                        .statusCode(200)
                        .body("cacheId", Matchers.comparesEqualTo(KNOWN_CACHE_ID))
                        .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"))
                        .body("items", Matchers.hasSize(getKnownItems().size()))
                        .body("items.find { it.key == 'Key1' }.value", Matchers.equalTo(getKnownItems().get("Key1")))
                        .body("items.find { it.key == 'Key2' }.value", Matchers.equalTo(getKnownItems().get("Key2")))
                        .body("items.find { it.key == 'Key3' }.value", Matchers.equalTo(getKnownItems().get("Key3"))));
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
