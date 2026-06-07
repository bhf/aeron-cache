package com.bhf.aeroncache.integration.shutdown;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import com.bhf.aeroncache.integration.utils.ContainerRestartUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Restart tests for the ephemeral cache mode.
 */
@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralCacheRestartTests {

    private static final String GET_ENDPOINT = "/api/v1/cache/";
    static final String KNOWN_CACHE_ID = "1★";
    static final String KNOWN_KEY = "SomeKey★★★";
    static final String KNOWN_VALUE = "SomeValue★★★";


    @Test
    @DisplayName("Should get known non-expired value we added post restart")
    @HappyPath
    void shouldGetKnownItemPostRestart(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);

        // Act
        ContainerRestartUtils.stopHTTPInterface(backend);
        ContainerRestartUtils.stopEphemeralContainers(backend);
        ContainerRestartUtils.awaitEphemeralAeronCacheClusterRestart(backend);

        var postRestartMappedHostDetails = ContainerRestartUtils.awaitHTTPInterfaceRestart(backend);
        var mappedPort = postRestartMappedHostDetails.mappedPort();
        var mappedHost = postRestartMappedHostDetails.mappedHost();

        // Assert
        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)
                .then().assertThat()
                .statusCode(404);

    }

}
