package com.bhf.aeroncache.integration.config;

public class SSECountersCacheTestEndpoints  implements StreamingTestEndpointsProvider {
    private static final String STREAMING_API_PREFIX = "/api/sse/v1/counter/";
    private static final String STREAMING_MULTI_CACHE_API_PREFIX = "/api/sse/v1/counters/";
    private static final String STREAMING_HYDRATE_API_PREFIX = "/api/sse/v1/counter/hydrate/";
    private static final String STREAMING_MULTI_CACHE_HYDRATE_API_PREFIX = "/api/sse/v1/counters/hydrate/";

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
