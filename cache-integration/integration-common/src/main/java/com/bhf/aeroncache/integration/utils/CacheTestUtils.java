package com.bhf.aeroncache.integration.utils;

import com.bhf.aeroncache.integration.BackendTestResource;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

/**
 * Utilities for setting up test scenarios.
 */
public class CacheTestUtils {

    private static final String CREATE_ENDPOINT = "/api/v1/cache/";
    private static final String PUT_ITEM_ENDPOINT = "/api/v1/cache/";
    private static final String DELETE_ENDPOINT = "/api/v1/cache/";
    private static final String CLEAR_ENDPOINT = "/api/v1/cache/";

    /**
     * Create a cache as part of setting up a test case.
     *
     * @param cacheId The ID of the cache to create.
     */
    public static void createCache(String cacheId, BackendTestResource backend) {
        JSONObject jsonObj = new JSONObject().put("cacheId", cacheId);
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, CREATE_ENDPOINT);
    }

    /**
     * Add an item to a cache as part of setting up a test case.
     *
     * @param cacheId The ID of the cache to add an item too.
     * @param key     The key to add the item against.
     * @param value   The value to add.
     */
    public static void addItem(String cacheId, String key, String value, BackendTestResource backend) {
        JSONObject jsonObj = new JSONObject()
                .put("key", key)
                .put("value", value);

        var endpoint = PUT_ITEM_ENDPOINT + cacheId;
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, endpoint);
    }

    /**
     * Remove an item from a cache as part of setting up a test scenario.
     *
     * @param cacheId The cache from which to remove the item.
     * @param key     The key of the item to be removed.
     */
    public static void removeItem(String cacheId, String key, BackendTestResource backend) {
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .request(Method.DELETE, DELETE_ENDPOINT + cacheId + "/" + key);
    }

    /**
     * Delete a cache as part of setting up a test scenario.
     *
     * @param cacheId THe cache to be deleted.
     */
    public static void deleteCache(String cacheId, BackendTestResource backend) {
        JSONObject jsonObj = new JSONObject().put("cacheId", cacheId);
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.DELETE, DELETE_ENDPOINT + cacheId);
    }

    /**
     * Clear the contents of a cache.
     *
     * @param cacheId
     * @param backend
     */
    public static void clearCache(String cacheId, BackendTestResource backend) {
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .request(Method.PATCH, CLEAR_ENDPOINT + cacheId);
    }
}
