package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
abstract class RemoveItemTests<V> {

    private final String REMOVE_ITEM_ENDPOINT;
    private static final String KNOWN_CACHE_ID = "1";
    private static final String UNKNOWN_CACHE_ID = "123";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String UNKNOWN_KEY = "UnknownKey";
    private static TestEndpointsProvider removeItemsEndpoints;

    RemoveItemTests(TestEndpointsProvider endpointsProvider) {
        REMOVE_ITEM_ENDPOINT = endpointsProvider.getRemoveItemEndpoint();
        removeItemsEndpoints = endpointsProvider;
    }

    abstract V getKnownValue();

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, removeItemsEndpoints);
    }

    @Test
    @DisplayName("Should remove an existing item")
    @HappyPath
    void shouldRemoveExistingItem(BackendTestResource backend) {
        // Arrange
        var value = getKnownValue();
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, value, backend, removeItemsEndpoints);

        JSONObject requestBody = new JSONObject().put("cacheId", KNOWN_CACHE_ID)
                .put("key", KNOWN_KEY)
                .put("value", value);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().delete(REMOVE_ITEM_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should get 404 on unknown cache")
    void shouldGetNAOnUnknownCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID, backend, removeItemsEndpoints);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().delete(REMOVE_ITEM_ENDPOINT + UNKNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should get 404 on unknown key")
    void shouldGet404OnUnknownKey(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.removeItem(KNOWN_CACHE_ID, UNKNOWN_KEY, backend, removeItemsEndpoints);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().delete(REMOVE_ITEM_ENDPOINT + KNOWN_CACHE_ID + "/" + UNKNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404);
    }

}
