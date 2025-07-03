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

class PutItemTests {

    private static final String PUT_ITEM_ENDPOINT = "/api/v1/cache/";
    private static final int KNOWN_CACHE_ID = 1;
    private static final int UNKNOWN_CACHE_ID = 123;
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7070;
        CacheTestUtils.createCache(KNOWN_CACHE_ID);
    }

    public static Stream<Arguments> provideBadParamsToPutItem() {
        return Stream.of(
                Arguments.of("wrongFieldName", 1),
                Arguments.of("cacheId", "not a number"));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for badly formed requests to put an item")
    @MethodSource("provideBadParamsToPutItem")
    void shouldReturn400ForBadlyFormedRequest(String field, Object value) {
        var requestBody = new JSONObject().put(field, value);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(PUT_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.notNullValue())
                .body("helpMsg", Matchers.notNullValue())
                .body("operationStatus", Matchers.notNullValue());
    }

    @Test
    @DisplayName("Should add an item to a known cache")
    @HappyPath
    void shouldAddAnItemToCache() {
        // Arrange
        JSONObject requestBody = new JSONObject().put("cacheId", KNOWN_CACHE_ID)
                .put("key", KNOWN_KEY)
                .put("value", KNOWN_VALUE);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(PUT_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should get 404 on unknown cache")
    void shouldGet404OnUnknownCache() {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID);
        JSONObject requestBody = new JSONObject().put("cacheId", UNKNOWN_CACHE_ID)
                .put("key", KNOWN_KEY)
                .put("value", KNOWN_VALUE);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(PUT_ITEM_ENDPOINT + UNKNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(404);
    }

}
