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
 * Integration test to verify that the HTTP interface can handle the ephemeral cache
 * being restarted without itself being restarted.
 */
@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class HttpClientEphemeralRestartTests {

    private static final String GET_ENDPOINT = "/api/v1/cache/";
    static final String KNOWN_CACHE_ID = "HttpRestart★";
    static final String KNOWN_KEY = "HttpRestartKey★★★";
    static final String KNOWN_VALUE = "HttpRestartValue★★★";

    @Test
    @DisplayName("Should perform operations via HTTP interface after ephemeral cache restart")
    @HappyPath
    void shouldHandleClusterRestart(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);

        // Act
        ContainerRestartUtils.stopEphemeralContainers(backend);
        ContainerRestartUtils.awaitEphemeralAeronCacheClusterRestart(backend);

        // Use readiness endpoint to wait for the HTTP interface to detect the reconnection to the cluster
        ContainerRestartUtils.awaitHTTPReadiness(backend);

        // Assert
        var mappedPort = backend.getHttpPort();
        var mappedHost = backend.getBaseHttpUri();

        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)
                .then().assertThat()
                .statusCode(404);

        // Assert - new operations
        var newKey = "NewKeyPostRestart";
        var newValue = "NewValuePostRestart";
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, newKey, newValue, backend);

        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + newKey)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(newValue));
    }
}
