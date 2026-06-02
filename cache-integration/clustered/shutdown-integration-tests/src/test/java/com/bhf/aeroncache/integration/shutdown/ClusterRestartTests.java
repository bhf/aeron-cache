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

@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusterRestartTests {

    private static final String GET_ENDPOINT = "/api/v1/cache/";
    static final String KNOWN_CACHE_ID = "1★";
    static final String KNOWN_KEY = "SomeKey★★★";
    static final String KNOWN_VALUE = "SomeValue★★★";
    static final String TTL_KEY = "TtlKey★★★";
    static final String TTL_VALUE = "TtlValue★★★";
    static final long TTL_MS = 2000L;

    @Test
    @DisplayName("Should get known non-expired value we added post restart")
    @HappyPath
    void shouldGetKnownItemPostRestart(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, TTL_KEY, TTL_VALUE, TTL_MS, backend);

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

    }

}
