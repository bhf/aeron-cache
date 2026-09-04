package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@ExtendWith(BackendTestLauncher.class)
abstract class DeleteCacheTests {

    private final String DELETE_CACHE_ENDPOINT;
    private static final String KNOWN_CACHE_ID = "1";
    private static final String UNKNOWN_CACHE_ID = "123";
    private final TestEndpointsProvider deleteEndpointProvider;

    DeleteCacheTests(TestEndpointsProvider deleteEndpoint) {
        DELETE_CACHE_ENDPOINT = deleteEndpoint.getDeleteEndpointCache();
        deleteEndpointProvider = deleteEndpoint;
    }

    @Test
    @DisplayName("Should delete an existing cache")
    @HappyPath
    void shouldDeleteExistingCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, deleteEndpointProvider);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().delete(DELETE_CACHE_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should return 404 on unknown cache")
    @HappyPath
    void shouldHandleUnknownCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID, backend, deleteEndpointProvider);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().delete(DELETE_CACHE_ENDPOINT + UNKNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(404);
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for badly formed requests to delete a cache")
    @MethodSource("provideBadParamsToDeleteCache")
    void shouldReturn400ForBadlyFormedRequest(String field, Object value, BackendTestResource backend) {
        var requestBody = new JSONObject().put(field, value);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(DELETE_CACHE_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.notNullValue())
                .body("helpMsg", Matchers.notNullValue())
                .body("operationStatus", Matchers.notNullValue());
    }

    public static Stream<Arguments> provideBadParamsToDeleteCache() {
        return Stream.of(
                Arguments.of("unknownField", "not a number"));
    }

}
