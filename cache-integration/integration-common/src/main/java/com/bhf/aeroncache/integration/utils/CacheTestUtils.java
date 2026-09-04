package com.bhf.aeroncache.integration.utils;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.json.JSONArray;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

/**
 * Utilities for setting up test scenarios.
 */
public class CacheTestUtils {

    /**
     * Create a cache as part of setting up a test case.
     *
     * @param cacheId The ID of the cache to create.
     */
    public static void createCache(String cacheId, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        JSONObject jsonObj = new JSONObject().put("cacheId", cacheId);
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, endpointsProvider.getCreateEndpointCache());
    }

    /**
     * Add an item to a cache as part of setting up a test case.
     *
     * @param cacheId The ID of the cache to add an item too.
     * @param key     The key to add the item against.
     * @param value   The value to add.
     */
    public static <V> void addItem(String cacheId, String key, V value, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        JSONObject jsonObj = new JSONObject()
                .put("key", key)
                .put("value", value);

        var endpoint = endpointsProvider.getPutItemEndpointCache() + cacheId;
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, endpoint);
    }

    /**
     * Add an item to a cache as part of setting up a test case.
     *
     * @param cacheId The ID of the cache to add an item too.
     * @param key     The key to add the item against.
     * @param value   The value to add.
     * @param ttl     The TTL.
     * @param backend Environment to use.
     */
    public static <V> void addItem(String cacheId, String key, V value, long ttl, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        JSONObject jsonObj = new JSONObject()
                .put("key", key)
                .put("value", value)
                .put("ttl", ttl);

        var endpoint = endpointsProvider.getPutItemEndpointCache() +"timed/"+cacheId;
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, endpoint);
    }

    /**
     * Increment a counter in a cache as part of setting up a test scenario.
     *
     * @param cacheId The ID of the counter cache holding the counter.
     * @param key     The key of the counter to increment.
     * @param amount  The amount to increment the counter by.
     */
    public static void incrementCounter(String cacheId, String key, long amount, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        JSONObject jsonObj = new JSONObject()
                .put("key", key)
                .put("amount", amount);

        var endpoint = endpointsProvider.getPutItemEndpointCache() + "increment/" + cacheId;
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, endpoint);
    }

    /**
     * Decrement a counter in a cache as part of setting up a test scenario.
     *
     * @param cacheId The ID of the counter cache holding the counter.
     * @param key     The key of the counter to decrement.
     * @param amount  The amount to decrement the counter by.
     */
    public static void decrementCounter(String cacheId, String key, long amount, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        JSONObject jsonObj = new JSONObject()
                .put("key", key)
                .put("amount", amount);

        var endpoint = endpointsProvider.getPutItemEndpointCache() + "decrement/" + cacheId;
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.POST, endpoint);
    }

    /**
     * Set a counter in a cache to a given value as part of setting up a test scenario.
     *
     * @param cacheId The ID of the counter cache holding the counter.
     * @param key     The key of the counter to set.
     * @param value   The value to set the counter to.
     */
    public static void setCounter(String cacheId, String key, long value, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        JSONObject jsonObj = new JSONObject()
                .put("key", key)
                .put("value", value);

        var endpoint = endpointsProvider.getPutItemEndpointCache() + "set/" + cacheId;
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
    public static void removeItem(String cacheId, String key, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .request(Method.DELETE, endpointsProvider.getDeleteEndpointCache() + cacheId + "/" + key);
    }

    /**
     * Delete a cache as part of setting up a test scenario.
     *
     * @param cacheId THe cache to be deleted.
     */
    public static void deleteCache(String cacheId, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        JSONObject jsonObj = new JSONObject().put("cacheId", cacheId);
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(jsonObj.toString())
                .request(Method.DELETE, endpointsProvider.getDeleteEndpointCache() + cacheId);
    }

    /**
     * Clear the contents of a cache.
     *
     * @param cacheId
     * @param backend
     */
    public static void clearCache(String cacheId, BackendTestResource backend, TestEndpointsProvider endpointsProvider) {
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .request(Method.PATCH, endpointsProvider.getClearEndpointCache() + cacheId);
    }

    public static void sendBulkRequest(BulkCacheOpsRequest request, BackendTestResource backend, TestEndpointsProvider endpointsProvider){
        var allOps = new JSONArray();
        for(var op : request.operations()){
            allOps.put(getCacheOperation(op.requestId(), op.operationType().toString(), op.cacheId(), op.key(), op.value(), op.ttl(), op.counterValue()));
        }

        JSONObject requestBody = new JSONObject()
                .put("requestId", "bulk-request-1")
                .put("operations", allOps);

        var endpoint = endpointsProvider.getBulkItemEndpointCache();
        given().port(backend.getHttpPort()).baseUri(backend.getBaseHttpUri())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody.toString())
                .request(Method.POST, endpoint);
    }


    public static JSONObject getCacheOperation(String requestId, String opType, String cacheId, String key, String value, long ttl, long counterValue) {
        return new JSONObject()
                .put("requestId", requestId)
                .put("operationType", opType)
                .put("cacheId", cacheId)
                .put("key", key)
                .put("value", value)
                .put("counterValue", counterValue)
                .put("ttl", ttl);
    }
}
