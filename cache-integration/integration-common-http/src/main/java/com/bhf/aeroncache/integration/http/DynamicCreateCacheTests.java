package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@RequiredArgsConstructor
@ExtendWith(BackendTestLauncher.class)
abstract class DynamicCreateCacheTests {

    private static final String BASE_API = "/api/v1/cache/";
    private static final String DYNAMIC_CACHE_ID = "12";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";
    private final TestEndpointsProvider endpointsProvider;

    @Test
    @DisplayName("Should create cache dynamically")
    @HappyPath
    void shouldCreateCacheDynamically(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(DYNAMIC_CACHE_ID, backend, endpointsProvider);

        var addItemRequestBody = new JSONObject()
                .put("key", KNOWN_KEY)
                .put("value", KNOWN_VALUE);

        // Act & Assert
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addItemRequestBody.toString())
                .when().post(BASE_API + DYNAMIC_CACHE_ID)
                .then().assertThat()
                .statusCode(200);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(BASE_API + DYNAMIC_CACHE_ID + "/" + KNOWN_KEY)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(KNOWN_VALUE));
    }

    @Test
    @DisplayName("Should create cache dynamically in a bulk op")
    @HappyPath
    void shouldCreateCacheDynamicallyInBulkOp(BackendTestResource backend) {
        // Arrange
        var dynamicBulkCacheId = "bulk-dynamic-1";
        CacheTestUtils.deleteCache(dynamicBulkCacheId, backend, endpointsProvider);

        var op1 = CacheTestUtils.getCacheOperation("req-1", "ADD_ITEM", dynamicBulkCacheId, KNOWN_KEY, KNOWN_VALUE, 0);
        var op2 = CacheTestUtils.getCacheOperation("req-2", "ADD_ITEM", dynamicBulkCacheId, "AnotherKey", "AnotherValue", 0);
        var ops = new JSONArray().put(op1).put(op2);
        
        var requestBody = new JSONObject()
                .put("requestId", "bulk-req-dynamic")
                .put("operations", ops);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .when().post("/api/v1/cache/bulkops/")

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("requestId", Matchers.comparesEqualTo("bulk-req-dynamic"))
                .body("operationResponses", Matchers.hasSize(2))
                .body("operationResponses[0].status", Matchers.comparesEqualTo("SUCCESS"))
                .body("operationResponses[1].status", Matchers.comparesEqualTo("SUCCESS"));

        // Verify items were actually added
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(BASE_API + dynamicBulkCacheId + "/" + KNOWN_KEY)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo(KNOWN_VALUE));

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(BASE_API + dynamicBulkCacheId + "/AnotherKey")
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.comparesEqualTo("AnotherValue"));
    }

}
