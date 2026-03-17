package com.bhf.aeroncache.integration.shutdown;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusterRestartTests {

    private static final Logger log = LoggerFactory.getLogger(ClusterRestartTests.class);
    private static final String GET_ENDPOINT = "/api/v1/cache/";
    static final String PUT_ITEM_ENDPOINT = "/api/v1/cache/";
    static final String KNOWN_CACHE_ID = "1";
    static final String KNOWN_KEY = "SomeKey";
    static final String KNOWN_VALUE = "SomeValue";

    @Test
    @DisplayName("Should get a known value we added post restart")
    @HappyPath
    void shouldGetKnownItemPostRestart(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        JSONObject requestBody = new JSONObject().put("cacheId", KNOWN_CACHE_ID)
                .put("key", KNOWN_KEY)
                .put("value", KNOWN_VALUE);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .when().post(PUT_ITEM_ENDPOINT + KNOWN_CACHE_ID)
                .then().assertThat()
                .statusCode(200);

        // Act
        backend.getContainers().httpContainer().stop();
        backend.getContainers().clusterContainers().forEach(GenericContainer::stop);

        // Re-Arrange
        ContainerRestartUtils.awaitAeronCacheClusterRestart(backend);
        ContainerRestartUtils.awaitHTTPInterfaceRestart(backend);

        var mappedPort = backend.getContainers().httpContainer().getMappedPort(7070);
        var mappedHost = "http://"+backend.getContainers().httpContainer().getHost();

        // Assert
        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(KNOWN_VALUE));

    }
}
