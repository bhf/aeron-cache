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
abstract class IncrementCounterTests {

    final String INCREMENT_ITEM_ENDPOINT;
    static final String KNOWN_CACHE_ID = "1";
    static final String UNKNOWN_CACHE_ID = "123";
    static final String KNOWN_KEY = "SomeCounter";
    private final TestEndpointsProvider endpointsProvider;

    IncrementCounterTests(TestEndpointsProvider endpoint) {
        INCREMENT_ITEM_ENDPOINT = endpoint.getPutItemEndpointCache() + "increment/";
        endpointsProvider = endpoint;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, endpointsProvider);
    }

    public static Stream<Arguments> provideBadParamsToIncrement() {
        return Stream.of(
                Arguments.of("wrongFieldName", 1));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for badly formed requests to increment a counter")
    @MethodSource("provideBadParamsToIncrement")
    void shouldReturn400ForBadlyFormedRequest(String field, Object value, BackendTestResource backend) {
        var requestBody = new JSONObject().put(field, value);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(INCREMENT_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.notNullValue())
                .body("helpMsg", Matchers.notNullValue())
                .body("operationStatus", Matchers.notNullValue());
    }

    @Test
    @DisplayName("Should increment a counter on a known cache and return the new value")
    @HappyPath
    void shouldIncrementCounter(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, 0, backend, endpointsProvider);
        JSONObject requestBody = new JSONObject()
                .put("key", KNOWN_KEY)
                .put("amount", 5);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(INCREMENT_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("key", Matchers.equalTo(KNOWN_KEY))
                .body("value", Matchers.equalTo(5));
    }

    @Test
    @DisplayName("Should accumulate the value across multiple increments")
    @HappyPath
    void shouldAccumulateAcrossIncrements(BackendTestResource backend) {
        // Arrange
        var key = "AccumulatingCounter";
        CacheTestUtils.addItem(KNOWN_CACHE_ID, key, 0, backend, endpointsProvider);
        JSONObject requestBody = new JSONObject()
                .put("key", key)
                .put("amount", 3);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .when().post(INCREMENT_ITEM_ENDPOINT + KNOWN_CACHE_ID)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.equalTo(3));

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(INCREMENT_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.equalTo(6));
    }

    @Test
    @DisplayName("Should get 404 when incrementing a counter on an unknown cache")
    void shouldGet404OnUnknownCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID, backend, endpointsProvider);
        JSONObject requestBody = new JSONObject()
                .put("key", KNOWN_KEY)
                .put("amount", 1);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(INCREMENT_ITEM_ENDPOINT + UNKNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(404);
    }
}
