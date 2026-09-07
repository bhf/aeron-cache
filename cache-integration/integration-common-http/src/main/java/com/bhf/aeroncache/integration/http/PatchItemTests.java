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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
abstract class PatchItemTests {

    final String PATCH_ITEM_ENDPOINT;
    final String GET_ITEM_ENDPOINT;
    static final String KNOWN_CACHE_ID = "1";
    static final String UNKNOWN_CACHE_ID = "123";
    static final String KNOWN_KEY = "PatchKey";
    static final String UNKNOWN_KEY = "MissingKey";
    static final String INITIAL_VALUE = "{\"a\":1,\"b\":2}";
    static final String PATCH_VALUE = "{\"b\":3,\"c\":4}";
    static final String EXPECTED_MERGED_VALUE = "{\"a\":1,\"b\":3,\"c\":4}";
    private final TestEndpointsProvider patchEndpointsProvider;

    PatchItemTests(TestEndpointsProvider patchEndpoint) {
        PATCH_ITEM_ENDPOINT = patchEndpoint.getPatchItemEndpointCache();
        GET_ITEM_ENDPOINT = patchEndpoint.getItemEndpoint();
        patchEndpointsProvider = patchEndpoint;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, patchEndpointsProvider);
    }

    @Test
    @DisplayName("Should deep-merge a patch into an existing item in a known cache")
    @HappyPath
    void shouldPatchAnItemInCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, INITIAL_VALUE, backend, patchEndpointsProvider);
        JSONObject requestBody = new JSONObject().put("value", PATCH_VALUE);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .when().patch(PATCH_ITEM_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.equalTo(KNOWN_CACHE_ID))
                .body("key", Matchers.equalTo(KNOWN_KEY))
                .body("operationStatus", Matchers.equalTo("SUCCESS"));

        // Assert the value was merged (not replaced)
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .accept(ContentType.JSON)
                .when().get(GET_ITEM_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.equalTo(EXPECTED_MERGED_VALUE));
    }

    @Test
    @DisplayName("Should return 404 when patching an item in an unknown cache")
    void shouldGet404OnUnknownCache(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID, backend, patchEndpointsProvider);
        JSONObject requestBody = new JSONObject().put("value", PATCH_VALUE);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .when().patch(PATCH_ITEM_ENDPOINT + UNKNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("operationStatus", Matchers.equalTo("UNKNOWN_CACHE"));
    }

    @Test
    @DisplayName("Should return 404 when patching an unknown key in a known cache")
    void shouldGet404OnUnknownKey(BackendTestResource backend) {
        // Arrange
        JSONObject requestBody = new JSONObject().put("value", PATCH_VALUE);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .when().patch(PATCH_ITEM_ENDPOINT + KNOWN_CACHE_ID + "/" + UNKNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404)
                .body("operationStatus", Matchers.equalTo("UNKNOWN_KEY"));
    }

    @Test
    @DisplayName("Should return 400 for a badly formed patch request")
    void shouldReturn400ForBadlyFormedRequest(BackendTestResource backend) {
        // Arrange
        var requestBody = new JSONObject().put("wrongFieldName", 1);

        // Act
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .when().patch(PATCH_ITEM_ENDPOINT + KNOWN_CACHE_ID + "/" + KNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.notNullValue())
                .body("helpMsg", Matchers.notNullValue())
                .body("operationStatus", Matchers.notNullValue());
    }

}
