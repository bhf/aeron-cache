package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BackendTestLauncher.class)
abstract class DynamicCreateCacheTests {

    private static final String BASE_API = "/api/v1/cache/";
    private static final String DYNAMIC_CACHE_ID = "12";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    @Test
    @DisplayName("Should create cache dynamically")
    @HappyPath
    void shouldCreateCacheDynamically(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(DYNAMIC_CACHE_ID, backend);

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


    }

}
