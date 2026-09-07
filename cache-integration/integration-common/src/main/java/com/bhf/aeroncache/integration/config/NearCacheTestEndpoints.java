package com.bhf.aeroncache.integration.config;

public class NearCacheTestEndpoints implements TestEndpointsProvider {

    private static final String NEAR_CACHE_PREFIX = "/api/v1/near/cache/";
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
        return NEAR_CACHE_PREFIX;
    }

    @Override
    public String getPatchItemEndpointCache() {
        return REGULAR_CACHE_PREFIX;
    }
}
