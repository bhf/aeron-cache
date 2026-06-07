package com.bhf.aeroncache.integration.shutdown;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import com.bhf.aeroncache.integration.utils.ContainerRestartUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Note that tests for restart are combined in a single class in order to
 * have a faster integration test suite - the restart tests are typically the slowest
 * to complete so become the bottleneck.
 */
@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusterRestartTests {

    private static final String GET_ENDPOINT = "/api/v1/cache/";
    static final String KNOWN_CACHE_ID = "1★";
    static final String KNOWN_KEY = "SomeKey★★★";
    static final String KNOWN_VALUE = "SomeValue★★★";
    static final String TTL_KEY = "TtlKey★★★";
    static final String TTL_VALUE = "TtlValue★★★";
    static final String LONG_TTL_KEY = "NewTtlKey★★★";
    static final String LONG_TTL_VALUE = "NewTtlValue★★★";
    static final long TTL_MS = 2000L;
    static final long LONG_TTL_MS = 3600000L;
    static final long SHORT_TTL_MS = 1000L;

    @Test
    @DisplayName("Should get known non-expired value we added post restart")
    @HappyPath
    void shouldGetKnownItemPostRestart(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, TTL_KEY, TTL_VALUE, TTL_MS, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, LONG_TTL_KEY, LONG_TTL_VALUE, LONG_TTL_MS, backend);

        // Act
        ContainerRestartUtils.stopHTTPInterface(backend);

        ContainerRestartUtils.stopClusterContainers(backend);

        ContainerRestartUtils.awaitAeronCacheClusterRestart(backend);
        var postRestartMappedHostDetails = ContainerRestartUtils.awaitHTTPInterfaceRestart(backend);
        var mappedPort = postRestartMappedHostDetails.mappedPort();
        var mappedHost = postRestartMappedHostDetails.mappedHost();

        // Sleep for the same time as the TTL of the item just to be sure
        try {
            Thread.sleep(TTL_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Assert
        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(KNOWN_VALUE));

        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + TTL_KEY)
                .then().assertThat()
                .statusCode(404);

        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + LONG_TTL_KEY)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(LONG_TTL_VALUE));

        canModifyTtlPostRestart(backend, mappedPort, mappedHost);
    }

    /**
     * Check we can modify and expire an item post restart.
     *
     * @param backend
     * @param mappedPort
     * @param mappedHost
     */
    private static void canModifyTtlPostRestart(BackendTestResource backend, Integer mappedPort, String mappedHost) {
        // Arrange - modify the LONG_TTL to now become the SHORT_TTL so the item expires
        CacheTestUtils.addItem(KNOWN_CACHE_ID, LONG_TTL_KEY, LONG_TTL_VALUE, SHORT_TTL_MS, backend);

        // Act - sleep for the same time as the TTL of the item just to be sure
        try {
            Thread.sleep(SHORT_TTL_MS + 100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Assert
        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + LONG_TTL_KEY)
                .then().assertThat()
                .statusCode(404);
    }

}
