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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
abstract class PutItemTests<V> {

    final String PUT_ITEM_ENDPOINT;
    static final String KNOWN_CACHE_ID = "1";
    static final String UNKNOWN_CACHE_ID = "123";
    static final String KNOWN_KEY = "SomeKey";
    private final TestEndpointsProvider putItemsEndpointProvider;

    PutItemTests(TestEndpointsProvider putEndpoint) {
        PUT_ITEM_ENDPOINT = putEndpoint.getPutItemEndpointCache();
        putItemsEndpointProvider = putEndpoint;
    }

    abstract V getKnownValue();

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, putItemsEndpointProvider);
    }

    public static Stream<Arguments> provideBadParamsToPutItem() {
        return Stream.of(
                Arguments.of("wrongFieldName", 1));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for badly formed requests to put an item")
    @MethodSource("provideBadParamsToPutItem")
    void shouldReturn400ForBadlyFormedRequest(String field, Object value, BackendTestResource backend) {
        var requestBody = new JSONObject().put(field, value);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
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
    void shouldAddAnItemToCache(BackendTestResource backend) {
        // Arrange
        JSONObject requestBody = new JSONObject()
                .put("key", KNOWN_KEY)
                .put("value", getKnownValue());

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
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
    void shouldGet404OnUnknownCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID, backend, putItemsEndpointProvider);
        JSONObject requestBody = new JSONObject()
                .put("key", KNOWN_KEY)
                .put("value", getKnownValue());

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
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
