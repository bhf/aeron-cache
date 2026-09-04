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
abstract class SetCounterTests {

    final String SET_ITEM_ENDPOINT;
    static final String KNOWN_CACHE_ID = "1";
    static final String UNKNOWN_CACHE_ID = "123";
    static final String KNOWN_KEY = "SomeCounter";
    private final TestEndpointsProvider endpointsProvider;

    SetCounterTests(TestEndpointsProvider endpoint) {
        SET_ITEM_ENDPOINT = endpoint.getPutItemEndpointCache() + "set/";
        endpointsProvider = endpoint;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, endpointsProvider);
    }

    public static Stream<Arguments> provideBadParamsToSet() {
        return Stream.of(
                Arguments.of("wrongFieldName", 1));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for badly formed requests to set a counter")
    @MethodSource("provideBadParamsToSet")
    void shouldReturn400ForBadlyFormedRequest(String field, Object value, BackendTestResource backend) {
        var requestBody = new JSONObject().put(field, value);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(SET_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.notNullValue())
                .body("helpMsg", Matchers.notNullValue())
                .body("operationStatus", Matchers.notNullValue());
    }

    @Test
    @DisplayName("Should set a counter on a known cache and return the new value")
    @HappyPath
    void shouldSetCounter(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, 0, backend, endpointsProvider);
        JSONObject requestBody = new JSONObject()
                .put("key", KNOWN_KEY)
                .put("value", 42);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(SET_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("key", Matchers.equalTo(KNOWN_KEY))
                .body("value", Matchers.equalTo(42));
    }

    @Test
    @DisplayName("Should overwrite an existing counter value across multiple sets")
    @HappyPath
    void shouldOverwriteAcrossSets(BackendTestResource backend) {
        // Arrange
        var key = "OverwritingCounter";
        CacheTestUtils.addItem(KNOWN_CACHE_ID, key, 10, backend, endpointsProvider);
        JSONObject firstBody = new JSONObject()
                .put("key", key)
                .put("value", 3);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(firstBody.toString())
                .when().post(SET_ITEM_ENDPOINT + KNOWN_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.equalTo(3));

        JSONObject secondBody = new JSONObject()
                .put("key", key)
                .put("value", 99);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(secondBody.toString())

                // Act
                .when().post(SET_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.equalTo(99));
    }

    @Test
    @DisplayName("Should get 404 when setting a counter on an unknown cache")
    void shouldGet404OnUnknownCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID, backend, endpointsProvider);
        JSONObject requestBody = new JSONObject()
                .put("key", KNOWN_KEY)
                .put("value", 1);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(SET_ITEM_ENDPOINT + UNKNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(404);
    }
}
