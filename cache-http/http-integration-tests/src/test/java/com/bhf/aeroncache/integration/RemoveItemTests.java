package com.bhf.aeroncache.integration;

import com.bhf.aeroncache.annotations.HappyPath;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

class RemoveItemTests {

    private static final String REMOVE_ITEM_ENDPOINT = "/api/v1/cache/";
    private static final String KNOWN_CACHE_ID = "1";
    private static final String UNKNOWN_CACHE_ID = "123";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String UNKNOWN_KEY = "UnknownKey";
    private static final String KNOWN_VALUE = "SomeValue";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7070;
        CacheTestUtils.createCache(KNOWN_CACHE_ID);
    }

    @Test
    @DisplayName("Should remove an existing item")
    @HappyPath
    void shouldRemoveExistingItem() {
        // Arrange
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE);

        JSONObject requestBody = new JSONObject().put("cacheId", KNOWN_CACHE_ID)
                .put("key", KNOWN_KEY)
                .put("value", KNOWN_VALUE);

        given()
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
    void shouldGetNAOnUnknownCache() {
        // Arrange
        CacheTestUtils.deleteCache(UNKNOWN_CACHE_ID);

        given()
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
    void shouldGet404OnUnknownKey() {
        // Arrange
        CacheTestUtils.removeItem(KNOWN_CACHE_ID, UNKNOWN_KEY);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

                // Act
                .when().delete(REMOVE_ITEM_ENDPOINT + KNOWN_CACHE_ID + "/" + UNKNOWN_KEY)

                // Assert
                .then().assertThat()
                .statusCode(404);
    }

}
