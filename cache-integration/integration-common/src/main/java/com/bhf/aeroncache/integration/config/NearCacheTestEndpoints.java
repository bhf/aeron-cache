package com.bhf.aeroncache.integration.config;

/**
 * Endpoints for exercising the near cache from integration tests. On an interface with
 * {@code NEAR_CACHE_ENABLED=true} the near cache is transparent: reads are served from the near cache
 * on the <em>regular</em> {@code /api/v1/cache/} read URIs, so every endpoint here resolves to the
 * regular prefix. The near-specific management operations (list-warm, stop) live under
 * {@code /api/v1/near/...} and are driven directly by the management tests rather than this provider.
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
