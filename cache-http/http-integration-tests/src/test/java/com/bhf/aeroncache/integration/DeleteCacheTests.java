package com.bhf.aeroncache.integration;

import com.bhf.aeroncache.annotations.HappyPath;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

class DeleteCacheTests {

    private static final String DELETE_CACHE_ENDPOINT = "/api/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";
    private static final String UNKNOWN_CACHE_ID = "123";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7070;
    }

    @Test
    @DisplayName("Should delete an existing cache")
    @HappyPath
    void shouldDeleteExistingCache() {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID);

        given()
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
    void shouldHandleUnknownCache() {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID);

        given()
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
    void shouldReturn400ForBadlyFormedRequest(String field, Object value) {
        var requestBody = new JSONObject().put(field, value);

        given()
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
