package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

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
    @HappyPath
    void shouldAddAnItemToCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        JSONObject requestBody = new JSONObject().put("cacheId", KNOWN_CACHE_ID)
                .put("key", KNOWN_KEY)
                .put("value", KNOWN_VALUE);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(PUT_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200);

        backend.getContainers().httpContainer().stop();
        backend.getContainers().clusterContainers().forEach(GenericContainer::stop);

        backend.getContainers().clusterContainers().forEach(this::startWithRetry);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int clusterNodesLaunched = 0;

        while (clusterNodesLaunched != 3) {
            int nodesUp = 0;

            for (var container : backend.getContainers().clusterContainers()) {
                if (container.isRunning()) {
                    nodesUp++;
                }
            }
            if (nodesUp < 3) {
                log.info("Only {} cache nodes up", nodesUp);
                backend.getContainers().clusterContainers().forEach(this::startWithRetry);
            } else {
                log.info("All cache nodes started and running");
                clusterNodesLaunched = nodesUp;
            }
        }

        backend.getContainers().httpContainer().start();
        startWithRetry(backend.getContainers().httpContainer());
        backend.getContainers().httpContainer().waitingFor(Wait.forHttp("/readiness"));

        var mappedPort = backend.getContainers().httpContainer().getMappedPort(7070);
        var mappedHost = "http://"+backend.getContainers().httpContainer().getHost();

        RestAssured.given().port(mappedPort)
                .baseUri(mappedHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(KNOWN_VALUE));


    }

    private void startWithRetry(GenericContainer<?> container) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (!container.isRunning()) {
                    log.info("Starting container {}, attempt {}", container.getDockerImageName(), i);
                    container.stop();
                    container.start();
                }
                if (container.isRunning()) {
                    return;
                }
            } catch (Exception e) {
                log.error("Couldn't start container {} on attempt {}", container.getDockerImageName(), i, e);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
