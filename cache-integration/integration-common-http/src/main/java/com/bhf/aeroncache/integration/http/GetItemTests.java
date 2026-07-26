package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
abstract class GetItemTests {

    //static String GET_ENDPOINT = "/api/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";
    private static final String UNKNOWN_CACHE_ID = "123";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";
    private static final String UNKNOWN_KEY = "UNKNOWN_KEY";

    private final String GET_ENDPOINT;
    private static TestEndpointsProvider getEndpointsProvider;

    GetItemTests(TestEndpointsProvider getEndpoint) {
        GET_ENDPOINT = getEndpoint.getItemEndpoint();
        getEndpointsProvider = getEndpoint;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {

        // seed the cache with a single cache and a known key-value pair
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, getEndpointsProvider);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend, getEndpointsProvider);
    }

    @Test
    @DisplayName("Should get an existing value back")
    @HappyPath
    void shouldGetExistingCacheValue(BackendTestResource backend) {
        // Arrange
        RestAssured.given().port(getHttpPort(backend)).baseUri(getHttpUri(backend))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(KNOWN_VALUE));
    }

    @Test
    @DisplayName("Should get a blank value back on an unknown key")
    void shouldGetBlankValueOnUnknownKey(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.removeItem(KNOWN_CACHE_ID, UNKNOWN_KEY, backend, getEndpointsProvider);

        RestAssured.given().port(getHttpPort(backend)).baseUri(getHttpUri(backend))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + UNKNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("value", Matchers.comparesEqualTo(""));
    }

    @Test
    @DisplayName("Should get 404 on unknown cache")
    void shouldGet404OnUnknownCache(BackendTestResource backend) {
        // Arrange
        RestAssured.given().port(getHttpPort(backend)).baseUri(getHttpUri(backend))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(GET_ENDPOINT + UNKNOWN_CACHE_ID + "/" + UNKNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("cacheId", Matchers.comparesEqualTo("0"))
                .body("key", Matchers.comparesEqualTo("NA"))
                .body("value", Matchers.comparesEqualTo("NA"));
    }

    String getHttpUri(BackendTestResource backend) {
        return backend.getBaseHttpUri();
    }

    int getHttpPort(BackendTestResource backend) {
        return backend.getHttpPort();
    }

}
