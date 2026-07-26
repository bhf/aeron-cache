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

import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
abstract class GetCacheTests {

    private static final String KNOWN_CACHE_ID = "GetCacheTestId";
    private static final String UNKNOWN_CACHE_ID = "UnknownCacheId";
    private static final Map<String, String> KNOWN_ITEMS = Map.of(
            "Key1", "Value1",
            "Key2", "Value2",
            "Key3", "Value3"
    );

    protected final String getCacheEntriesEndpoint;
    private static TestEndpointsProvider getCacheEndpointsProvider;

    protected GetCacheTests(TestEndpointsProvider getEndpoint) {
        getCacheEntriesEndpoint = getEndpoint.getItemEndpoint();
        getCacheEndpointsProvider = getEndpoint;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, getCacheEndpointsProvider);
        KNOWN_ITEMS.forEach((key, value) -> CacheTestUtils.addItem(KNOWN_CACHE_ID, key, value, backend, getCacheEndpointsProvider));
    }

    @Test
    @DisplayName("Should get all items from an existing cache")
    @HappyPath
    void shouldGetAllCacheEntries(BackendTestResource backend) {
        // Arrange
        RestAssured.given().port(getHttpPort(backend)).baseUri(getHttpUri(backend))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(getCacheEntriesEndpoint + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(KNOWN_CACHE_ID))
                .body("operationStatus", Matchers.comparesEqualTo("SUCCESS"))
                .body("items", Matchers.hasSize(KNOWN_ITEMS.size()))
                .body("items.find { it.key == 'Key1' }.value", Matchers.comparesEqualTo("Value1"))
                .body("items.find { it.key == 'Key2' }.value", Matchers.comparesEqualTo("Value2"))
                .body("items.find { it.key == 'Key3' }.value", Matchers.comparesEqualTo("Value3"));
    }

    @Test
    @DisplayName("Should get 404 on unknown cache")
    void shouldGetErrorForUnknownCache(BackendTestResource backend) {
        // Arrange
        RestAssured.given().port(getHttpPort(backend)).baseUri(getHttpUri(backend))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().get(getCacheEntriesEndpoint + UNKNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("operationStatus", Matchers.comparesEqualTo("UNKNOWN_CACHE"));
    }

    String getHttpUri(BackendTestResource backend) {
        return backend.getBaseHttpUri();
    }

    int getHttpPort(BackendTestResource backend) {
        return backend.getHttpPort();
    }

}
