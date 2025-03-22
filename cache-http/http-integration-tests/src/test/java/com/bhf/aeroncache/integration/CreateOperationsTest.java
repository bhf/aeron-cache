package com.bhf.aeroncache.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;

class CreateOperationsTest {

    private static final String CREATE_ENDPOINT = "/api/v1/cache/";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7070;
    }

    public static Stream<Arguments> provideBadParamsToCreateCache() {
        return Stream.of(
                Arguments.of("wrongFieldName", 1),
                Arguments.of("cacheId", "not a number"));
    }

    @Test
    @DisplayName("Should create basic cache")
    void shouldCreateBasicCache() {
        // Arrange
        var cacheId = 1;
        JSONObject jsonObj = new JSONObject().put("cacheId", cacheId);

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(jsonObj.toString())

        // Act
        .when().post(CREATE_ENDPOINT)

        // Assert
        .then().assertThat()
            .statusCode(200)
            .body("cacheId", comparesEqualTo(cacheId));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for badly formed requests to create a cache")
    @MethodSource("provideBadParamsToCreateCache")
    void shouldReturn400ForBadlyFormedCreateCacheRequest(String field, Object value){
        var message = new JSONObject().put(field, value);

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(message.toString())

        // Act
        .when().post(CREATE_ENDPOINT)

        // Assert
        .then().assertThat()
           .statusCode(400);
    }

}
