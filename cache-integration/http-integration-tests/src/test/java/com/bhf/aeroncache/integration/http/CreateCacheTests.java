package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;

@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false)
class CreateCacheTests {

    private static final String CREATE_ENDPOINT = "/api/v1/cache/";

    @Test
    @DisplayName("Should create basic cache")
    @HappyPath
    void shouldCreateBasicCache(BackendTestResource backend) {
        // Arrange
        var cacheId = "12";
        CacheTestUtils.deleteCache(cacheId, backend);

        JSONObject requestBody = new JSONObject().put("cacheId", cacheId);

        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(CREATE_ENDPOINT)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", comparesEqualTo(cacheId));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for badly formed requests to create a cache")
    @MethodSource("provideBadParamsToCreateCache")
    void shouldReturn400ForBadlyFormedCreateCacheRequest(String field, Object value, BackendTestResource backend) {
        var requestBody = new JSONObject().put(field, value);

        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())

                // Act
                .when().post(CREATE_ENDPOINT)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.notNullValue())
                .body("helpMsg", Matchers.notNullValue())
                .body("operationStatus", Matchers.notNullValue());
    }

    public static Stream<Arguments> provideBadParamsToCreateCache() {
        return Stream.of(
                Arguments.of("wrongFieldName", 1));
    }

}
