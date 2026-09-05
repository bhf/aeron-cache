package com.bhf.aeroncache.integration.http;

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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Tests putting and retrieving string key-value pairs of varying value sizes,
 * ranging from a few bytes up to approximately 16MB (the Aeron term buffer length
 * of {@code 16777216} bytes).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
abstract class LargeItemTests {

    static final int SIXTEEN_MB = 16 * 1024 * 1024;
    static final String KNOWN_CACHE_ID = "1";

    private final String PUT_ITEM_ENDPOINT;
    private final String GET_ITEM_ENDPOINT;
    private final TestEndpointsProvider endpointsProvider;

    LargeItemTests(TestEndpointsProvider endpoints) {
        PUT_ITEM_ENDPOINT = endpoints.getPutItemEndpointCache();
        GET_ITEM_ENDPOINT = endpoints.getItemEndpoint();
        endpointsProvider = endpoints;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, endpointsProvider);
    }

    static Stream<Arguments> provideValueSizes() {
        return Stream.of(
                Arguments.of("1B", 1),
                Arguments.of("1KB", 1024),
                Arguments.of("64KB", 64 * 1024),
                Arguments.of("256KB", 256 * 1024),
                Arguments.of("1MB", 1024 * 1024),
                Arguments.of("4MB", 4 * 1024 * 1024),
                Arguments.of("8MB", 8 * 1024 * 1024),
                Arguments.of("~16MB", SIXTEEN_MB - 1024));
    }

    @ParameterizedTest(name = "value of {0} ({1} bytes)")
    @DisplayName("Should store and retrieve a value of the given size")
    @MethodSource("provideValueSizes")
    void shouldStoreAndRetrieveValueOfSize(String label, int valueSize, BackendTestResource backend) {
        // Arrange
        String key = "largeItem-" + label;
        String value = "a".repeat(valueSize);

        JSONObject requestBody = new JSONObject()
                .put("key", key)
                .put("value", value);

        // Act - store the item
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .when().post(PUT_ITEM_ENDPOINT + KNOWN_CACHE_ID)

                // Assert
                .then().assertThat()
                .statusCode(200);

        // Act - retrieve the item and assert the round-trip value matches
        RestAssured.given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().get(GET_ITEM_ENDPOINT + KNOWN_CACHE_ID + "/" + key)

                // Assert
                .then().assertThat()
                .statusCode(200)
                .body("value", Matchers.equalTo(value));
    }

}
