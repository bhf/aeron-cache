package com.bhf.aeroncache.integration.streaming.near;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractStreamingNearCountersTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true,
        useTestContainersEnvironment = true, streamingNearCacheEnabled = true)
class ClusteredSSENearCountersTests extends AbstractStreamingNearCountersTests {

    public ClusteredSSENearCountersTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getCacheId() {
        return "clustered-near-counters-sse";
    }

    @Override
    protected String managementBaseUri(BackendTestResource backend) {
        return backend.getBaseSSEUri();
    }

    @Override
    protected int managementPort(BackendTestResource backend) {
        return backend.getSsePort();
    }
}
