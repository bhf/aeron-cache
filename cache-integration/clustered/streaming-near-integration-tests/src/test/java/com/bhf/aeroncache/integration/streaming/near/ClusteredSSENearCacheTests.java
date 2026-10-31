package com.bhf.aeroncache.integration.streaming.near;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractStreamingNearCacheTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true,
        useTestContainersEnvironment = true, streamingNearCacheEnabled = true)
class ClusteredSSENearCacheTests extends AbstractStreamingNearCacheTests {

    public ClusteredSSENearCacheTests() {
        super(new CacheTestEndpoints(), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getCacheId() {
        return "clustered-near-cache-sse";
    }

    @Override
    protected String managementBaseUri(BackendTestResource backend) {
        // The SSE base URI is already an http:// URI serving the near cache management endpoints.
        return backend.getBaseSSEUri();
    }

    @Override
    protected int managementPort(BackendTestResource backend) {
        return backend.getSsePort();
    }
}
