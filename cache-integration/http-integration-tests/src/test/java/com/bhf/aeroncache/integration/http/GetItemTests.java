package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;

@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false)
class GetItemTests {

    private static final String GET_ENDPOINT = "/api/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";
    private static final String UNKNOWN_CACHE_ID = "123";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";
    private static final String UNKNOWN_KEY = "UNKNOWN_KEY";

    @BeforeAll
    static void setup(BackendTestResource backend) {

        // seed the cache with a single cache and a known key-value pair
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);
    }

    @Test
    @DisplayName("Should get an existing value back")
    @HappyPath
    void shouldGetExistingCacheValue(BackendTestResource backend) {
        // Arrange
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("value", comparesEqualTo(KNOWN_VALUE));
    }

    @Test
    @DisplayName("Should get a blank value back on an unknown key")
    void shouldGetBlankValueOnUnknownKey(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.removeItem(KNOWN_CACHE_ID, UNKNOWN_KEY, backend);

        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(GET_ENDPOINT + KNOWN_CACHE_ID + "/" + UNKNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("value", comparesEqualTo(""));
    }

    @Test
    @DisplayName("Should get 404 on unknown cache")
    void shouldGet404OnUnknownCache(BackendTestResource backend) {
        // Arrange
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(GET_ENDPOINT + UNKNOWN_CACHE_ID + "/" + UNKNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("cacheId", comparesEqualTo("0"))
                .body("key", comparesEqualTo("NA"))
                .body("value", comparesEqualTo("NA"));
    }

}
