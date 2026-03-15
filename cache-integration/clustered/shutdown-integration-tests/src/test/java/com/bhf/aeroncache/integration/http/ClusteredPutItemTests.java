package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.testcontainers.containers.GenericContainer;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredPutItemTests extends PutItemTests{

    private static final String GET_ENDPOINT = "/api/v1/cache/";

    @Override
    void shouldAddAnItemToCache(BackendTestResource backend) {
        // Arrange
        super.shouldAddAnItemToCache(backend);

        backend.getContainers().httpContainer().stop();
        backend.getContainers().clusterContainers().forEach(GenericContainer::stop);

        backend.getContainers().clusterContainers().forEach(GenericContainer::start);
        backend.getContainers().httpContainer().start();

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(KNOWN_VALUE));


    }
}
