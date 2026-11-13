package com.bhf.aeroncache.integration.config;

/**
 * Endpoints for exercising the near cache from integration tests.
 */
public class NearCacheTestEndpoints implements TestEndpointsProvider {

    private static final String REGULAR_CACHE_PREFIX = "/api/v1/cache/";

    @Override
    public String getCreateEndpointCache() {
        return REGULAR_CACHE_PREFIX;
    }

    @Override
    public String getPutItemEndpointCache() {
        return REGULAR_CACHE_PREFIX;
    }

    @Override
    public String getDeleteEndpointCache() {
        return REGULAR_CACHE_PREFIX;
    }

    @Override
    public String getClearEndpointCache() {
        return REGULAR_CACHE_PREFIX;
    }

    @Override
    public String getBulkItemEndpointCache() {
        return REGULAR_CACHE_PREFIX + "bulkops/";
    }

    @Override
    public String getRemoveItemEndpoint() {
        return REGULAR_CACHE_PREFIX;
    }

    @Override
    public String getItemEndpoint() {
        return REGULAR_CACHE_PREFIX;
    }

    @Override
    public String getPatchItemEndpointCache() {
        return REGULAR_CACHE_PREFIX;
    }
}
