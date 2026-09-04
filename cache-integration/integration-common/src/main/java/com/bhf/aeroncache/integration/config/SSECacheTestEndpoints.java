package com.bhf.aeroncache.integration.config;

public class SSECacheTestEndpoints implements StreamingTestEndpointsProvider{

    private static final String STREAMING_API_PREFIX = "/api/sse/v1/cache/";
    private static final String STREAMING_MULTI_CACHE_API_PREFIX = "/api/sse/v1/caches/";
    private static final String STREAMING_HYDRATE_API_PREFIX = "/api/sse/v1/cache/hydrate/";
    private static final String STREAMING_MULTI_CACHE_HYDRATE_API_PREFIX = "/api/sse/v1/caches/hydrate/";

    public String getStreamingApiPrefix() {
        return STREAMING_API_PREFIX;
    }

    public String getStreamingMultiCacheApiPrefix() {
        return STREAMING_MULTI_CACHE_API_PREFIX;
    }

    public String getStreamingHydrateApiPrefix() {
        return STREAMING_HYDRATE_API_PREFIX;
    }

    public String getStreamingMultiCacheHydrateApiPrefix() {
        return STREAMING_MULTI_CACHE_HYDRATE_API_PREFIX;
    }
}
