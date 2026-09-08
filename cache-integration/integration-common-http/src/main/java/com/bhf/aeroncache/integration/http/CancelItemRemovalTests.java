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
abstract class CancelItemRemovalTests<V> {

    private final String CANCEL_REMOVAL_ENDPOINT;
    private static final String CANCEL_REMOVAL_SUFFIX = "/cancel-removal";
    private static final long TTL = 60000L;
    private static final String KNOWN_CACHE_ID = "1";
    private static final String UNKNOWN_CACHE_ID = "123";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String NO_TIMER_KEY = "NoTimerKey";
    private static TestEndpointsProvider endpointsProvider;

    CancelItemRemovalTests(TestEndpointsProvider endpoints) {
        CANCEL_REMOVAL_ENDPOINT = endpoints.getRemoveItemEndpoint();
        endpointsProvider = endpoints;
    }

    abstract V getKnownValue();

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, endpointsProvider);
    }

    @Test
    @DisplayName("Should cancel a scheduled removal for an existing item")
    @HappyPath
    void shouldCancelScheduledRemoval(BackendTestResource backend) {
        // Arrange - add an item with a ttl so a removal timer is scheduled
        var value = getKnownValue();
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, value, TTL, backend, endpointsProvider);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().post(CANCEL_REMOVAL_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY + CANCEL_REMOVAL_SUFFIX)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.equalTo(KNOWN_CACHE_ID))
                .body("key", Matchers.equalTo(KNOWN_KEY))
                .body("operationStatus", Matchers.equalTo("SUCCESS"));
    }

    @Test
    @DisplayName("Should get 404 when cancelling a removal in an unknown cache")
    void shouldGet404OnUnknownCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID, backend, endpointsProvider);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().post(CANCEL_REMOVAL_ENDPOINT + UNKNOWN_CACHE_ID + "/" + KNOWN_KEY + CANCEL_REMOVAL_SUFFIX)

                // Assert
                .then().assertThat()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should get 404 when no removal was scheduled for the key")
    void shouldGet404WhenNoRemovalScheduled(BackendTestResource backend) {
        // Arrange - add an item without a ttl so no removal timer is scheduled
        CacheTestUtils.addItem(KNOWN_CACHE_ID, NO_TIMER_KEY, getKnownValue(), backend, endpointsProvider);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().post(CANCEL_REMOVAL_ENDPOINT + KNOWN_CACHE_ID + "/" + NO_TIMER_KEY + CANCEL_REMOVAL_SUFFIX)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("operationStatus", Matchers.equalTo("UNKNOWN_KEY"));
    }

}
