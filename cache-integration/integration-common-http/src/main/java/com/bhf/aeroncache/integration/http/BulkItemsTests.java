package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONArray;
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
abstract class BulkItemsTests {

    final String BULK_ITEM_ENDPOINT;
    static final String KNOWN_CACHE_ID = "bulkops-test-cache";
    static final String UNKNOWN_CACHE_ID = "unknown-cache";
    static final String KNOWN_KEY = "SomeKey";
    static final String KNOWN_VALUE = "SomeValue";
    private static TestEndpointsProvider bulkItemsTestEndpoint;

    BulkItemsTests(TestEndpointsProvider bulkEndpoint) {
        BULK_ITEM_ENDPOINT = bulkEndpoint.getBulkItemEndpointCache();
        bulkItemsTestEndpoint = bulkEndpoint;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, bulkItemsTestEndpoint);
    }

    public static Stream<Arguments> provideBadParamsToBulkOps() {
        return Stream.of(
                Arguments.of("wrongFieldName", 1));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for badly formed requests to bulkops")
    @MethodSource("provideBadParamsToBulkOps")
    void shouldReturn400ForBadlyFormedRequest(String field, Object value, BackendTestResource backend) {
        var requestBody = new JSONObject().put(field, value);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(BULK_ITEM_ENDPOINT)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.notNullValue())
                .body("helpMsg", Matchers.notNullValue())
                .body("operationStatus", Matchers.notNullValue());
    }

    @Test
    @DisplayName("Should handle bulk request to a known cache")
    @HappyPath
    void shouldAddMultipleItemsToCache(BackendTestResource backend) {
        // Arrange

        var allOps = new JSONArray();
        allOps.put(CacheTestUtils.getCacheOperation("request-id-1", "ADD_ITEM", KNOWN_CACHE_ID, KNOWN_KEY + "-0", KNOWN_VALUE, 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("request-id-2", "ADD_ITEM", KNOWN_CACHE_ID, KNOWN_KEY + "-1", KNOWN_VALUE, 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("request-id-3", "ADD_ITEM", KNOWN_CACHE_ID, KNOWN_KEY + "-2", KNOWN_VALUE, 0, 0));

        JSONObject requestBody = new JSONObject()
                .put("requestId", "bulk-request-1")
                .put("operations", allOps);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(BULK_ITEM_ENDPOINT)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("requestId", Matchers.equalTo("bulk-request-1"))
                .body("operationResponses", Matchers.hasSize(3))

                .body("operationResponses[0].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[0].cacheId", Matchers.equalTo(KNOWN_CACHE_ID))
                .body("operationResponses[0].requestId", Matchers.equalTo("request-id-1"))
                .body("operationResponses[0].key", Matchers.equalTo(KNOWN_KEY+"-0"))

                .body("operationResponses[1].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[1].cacheId", Matchers.equalTo(KNOWN_CACHE_ID))
                .body("operationResponses[1].requestId", Matchers.equalTo("request-id-2"))
                .body("operationResponses[1].key", Matchers.equalTo(KNOWN_KEY+"-1"))

                .body("operationResponses[2].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[2].cacheId", Matchers.equalTo(KNOWN_CACHE_ID))
                .body("operationResponses[2].requestId", Matchers.equalTo("request-id-3"))
                .body("operationResponses[2].key", Matchers.equalTo(KNOWN_KEY+"-2"));
    }

    @Test
    @DisplayName("Should handle bulk request with add, get, and remove operations")
    @HappyPath
    void shouldAddGetAndRemoveItemsInBulk(BackendTestResource backend) {
        // Arrange
        var key = KNOWN_KEY + "-bulk-mixed-test";
        var value = KNOWN_VALUE + "-bulk-mixed-test";
        var requestId = "bulk-request-mixed";

        var allOps = new JSONArray();
        allOps.put(CacheTestUtils.getCacheOperation("addRequestId", "ADD_ITEM", KNOWN_CACHE_ID, key, value, 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("getRequestId", "GET_ITEM", KNOWN_CACHE_ID, key, "", 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("removeRequestId", "REMOVE_ITEM", KNOWN_CACHE_ID, key, "", 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("getAfterRemoveRequestId", "GET_ITEM", KNOWN_CACHE_ID, key, "", 0, 0));

        JSONObject requestBody = new JSONObject()
                .put("requestId", requestId)
                .put("operations", allOps);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(BULK_ITEM_ENDPOINT)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("requestId", Matchers.equalTo(requestId))
                .body("operationResponses", Matchers.hasSize(4))

                // Add item response
                .body("operationResponses[0].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[0].requestId", Matchers.equalTo("addRequestId"))
                .body("operationResponses[0].key", Matchers.equalTo(key))

                // Get item response
                .body("operationResponses[1].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[1].requestId", Matchers.equalTo("getRequestId"))
                .body("operationResponses[1].key", Matchers.equalTo(key))
                .body("operationResponses[1].value", Matchers.equalTo(value))

                // Remove item response
                .body("operationResponses[2].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[2].requestId", Matchers.equalTo("removeRequestId"))
                .body("operationResponses[2].key", Matchers.equalTo(key))

                // Get unknown item
                .body("operationResponses[3].status", Matchers.equalTo("UNKNOWN_KEY"))
                .body("operationResponses[3].requestId", Matchers.equalTo("getAfterRemoveRequestId"))
                .body("operationResponses[3].key", Matchers.equalTo(key));
    }

    @Test
    @DisplayName("Should deep-merge an item via a PATCH_ITEM operation in bulk")
    @HappyPath
    void shouldPatchItemInBulk(BackendTestResource backend) {
        // Arrange
        var key = KNOWN_KEY + "-bulk-patch-test";
        var initialValue = "{\"a\":1,\"b\":2}";
        var patchValue = "{\"b\":3,\"c\":4}";
        var mergedValue = "{\"a\":1,\"b\":3,\"c\":4}";
        var requestId = "bulk-request-patch";

        var allOps = new JSONArray();
        allOps.put(CacheTestUtils.getCacheOperation("addRequestId", "ADD_ITEM", KNOWN_CACHE_ID, key, initialValue, 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("patchRequestId", "PATCH_ITEM", KNOWN_CACHE_ID, key, patchValue, 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("getRequestId", "GET_ITEM", KNOWN_CACHE_ID, key, "", 0, 0));

        JSONObject requestBody = new JSONObject()
                .put("requestId", requestId)
                .put("operations", allOps);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(BULK_ITEM_ENDPOINT)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("requestId", Matchers.equalTo(requestId))
                .body("operationResponses", Matchers.hasSize(3))

                // Add item response
                .body("operationResponses[0].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[0].requestId", Matchers.equalTo("addRequestId"))
                .body("operationResponses[0].key", Matchers.equalTo(key))

                // Patch item response
                .body("operationResponses[1].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[1].requestId", Matchers.equalTo("patchRequestId"))
                .body("operationResponses[1].key", Matchers.equalTo(key))

                // Get item response confirms the value was merged, not replaced
                .body("operationResponses[2].status", Matchers.equalTo("SUCCESS"))
                .body("operationResponses[2].requestId", Matchers.equalTo("getRequestId"))
                .body("operationResponses[2].key", Matchers.equalTo(key))
                .body("operationResponses[2].value", Matchers.equalTo(mergedValue));
    }

    @Test
    @DisplayName("Should handle bulk request for unknown cache")
    @HappyPath
    void shouldHandleBulkRequestToUnknownCache(BackendTestResource backend) {
        // Arrange
        String testKey = KNOWN_KEY + "-bulk-unknown-test";
        String testValue = KNOWN_VALUE + "-bulk-unknown-test";

        var allOps = new JSONArray();
        allOps.put(CacheTestUtils.getCacheOperation("addRequestId", "ADD_ITEM", UNKNOWN_CACHE_ID, testKey, testValue, 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("getRequestId", "GET_ITEM", UNKNOWN_CACHE_ID, testKey, "", 0, 0));
        allOps.put(CacheTestUtils.getCacheOperation("removeRequestId", "REMOVE_ITEM", UNKNOWN_CACHE_ID, testKey, "", 0, 0));

        JSONObject requestBody = new JSONObject()
                .put("requestId", "bulk-request-unknown")
                .put("operations", allOps);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(BULK_ITEM_ENDPOINT)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("requestId", Matchers.equalTo("bulk-request-unknown"))
                .body("operationResponses", Matchers.hasSize(3))

                // Add item response
                .body("operationResponses[0].status", Matchers.equalTo("UNKNOWN_CACHE"))
                .body("operationResponses[0].requestId", Matchers.equalTo("addRequestId"))

                // Get item response
                .body("operationResponses[1].status", Matchers.equalTo("UNKNOWN_CACHE"))
                .body("operationResponses[1].requestId", Matchers.equalTo("getRequestId"))

                // Remove item response
                .body("operationResponses[2].status", Matchers.equalTo("UNKNOWN_CACHE"))
                .body("operationResponses[2].requestId", Matchers.equalTo("removeRequestId"));
    }

}
