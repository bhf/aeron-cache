package com.bhf.aeroncache.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;

class GetOperationsTest {

    private static final String CREATE_ENDPOINT = "/api/v1/cache/";
    private static final String PUT_ITEM_ENDPOINT = "/api/v1/cache/";
    private static final int KNOWN_CACHE_ID = 1;
    private static final int UNKNOWN_CACHE_ID = 123;
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";
    private static final String UNKNOWN_KEY = "UNKNOWN_KEY";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7070;

        // seed the cache with a single cache and a known key-value pair
        createCache(KNOWN_CACHE_ID);
        addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE);
    }

    private static void createCache(int cacheId) {
        JSONObject jsonObj = new JSONObject().put("cacheId", cacheId);
        var resp = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, CREATE_ENDPOINT);

        System.out.println("Got response on create cache: "+resp);
    }

    private static void addItem(int cacheId, String key, String value) {
        JSONObject jsonObj = new JSONObject()
                .put("cacheId", cacheId)
                .put("key", key)
                .put("value", value);

        var endpoint = PUT_ITEM_ENDPOINT+KNOWN_CACHE_ID;
        var resp = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, endpoint);

        System.out.println("Got response from adding item to cache: "+resp);
    }


    @Test
    @DisplayName("Should get an existing value back")
    void shouldGetExistingCacheValue() {
        // Arrange
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

        // Act
        .when().get("/api/v1/cache/"+KNOWN_CACHE_ID+"/"+KNOWN_KEY)

        // Assert
        .then().assertThat()
            .statusCode(200)
            .body("value", comparesEqualTo(KNOWN_VALUE));
    }

    @Test
    @DisplayName("Should get a blank value back on an unknown key")
    void shouldGetBlankValueOnUnknownKey() {
        // Arrange
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

        // Act
        .when().get("/api/v1/cache/"+KNOWN_CACHE_ID+"/"+UNKNOWN_KEY)

        // Assert
        .then().assertThat()
            .statusCode(200)
            .body("value", comparesEqualTo(""));
    }

    @Test
    @DisplayName("Should get NA on unknown cache")
    void shouldGetNAOnUnknownCache() {
        // Arrange
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

        // Act
        .when().get("/api/v1/cache/"+UNKNOWN_CACHE_ID+"/"+UNKNOWN_KEY)

        // Assert
        .then().assertThat()
            .statusCode(200)
            .body("cacheId", comparesEqualTo(0))
            .body("key", comparesEqualTo("NA"))
            .body("value", comparesEqualTo("NA"));
    }

}
