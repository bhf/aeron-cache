package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import com.bhf.aeroncache.models.ErrorMessages;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(BackendTestLauncher.class)
@RequiredArgsConstructor
abstract class CreateCacheTests {

    private final String CREATE_ENDPOINT;
    private final TestEndpointsProvider endpointsProvider;

    @Test
    @DisplayName("Should create basic cache")
    @HappyPath
    void shouldCreateBasicCache(BackendTestResource backend) {
        // Arrange
        var cacheId = "12";
        CacheTestUtils.deleteCache(cacheId, backend, endpointsProvider);

        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().put(CREATE_ENDPOINT + cacheId)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("cacheId", Matchers.comparesEqualTo(cacheId));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for cache ids containing special characters")
    @ValueSource(strings = {"bad.id", "bad@id", "bad+id"})
    void shouldReturn400ForCacheIdWithSpecialCharacters(String invalidCacheId, BackendTestResource backend) {
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().put(CREATE_ENDPOINT + invalidCacheId)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.comparesEqualTo("Cache ID shouldn't contain special characters"))
                .body("helpMsg", Matchers.comparesEqualTo(ErrorMessages.CACHE_ID_NO_SPECIAL_CHARACTERS))
                .body("operationStatus", Matchers.comparesEqualTo("ERROR"));
    }

    @ParameterizedTest
    @DisplayName("Should return status 400 for invalid cache names")
    @ValueSource(strings = {"bulkops", "timed"})
    void shouldReturn400ForInvalidCacheNames(String invalidCacheId, BackendTestResource backend) {
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().put(CREATE_ENDPOINT + invalidCacheId)

                // Assert
                .then().assertThat()
                .statusCode(400)
                .body("errorMsg", Matchers.comparesEqualTo("Cache ID shouldn't be a reserved name"))
                .body("helpMsg", Matchers.comparesEqualTo(ErrorMessages.CACHE_ID_NO_RESERVED_NAMES))
                .body("operationStatus", Matchers.comparesEqualTo("ERROR"));
    }

}
