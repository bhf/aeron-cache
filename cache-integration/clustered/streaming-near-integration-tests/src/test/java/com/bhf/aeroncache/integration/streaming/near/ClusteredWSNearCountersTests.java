package com.bhf.aeroncache.integration.streaming.near;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractStreamingNearCountersTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true,
        useTestContainersEnvironment = true, streamingNearCacheEnabled = true)
class ClusteredWSNearCountersTests extends AbstractStreamingNearCountersTests {

    public ClusteredWSNearCountersTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
    }

    @Override
    protected String getCacheId() {
        return "clustered-near-counters-ws";
    }

    @Override
    protected String managementBaseUri(BackendTestResource backend) {
        return backend.getBaseWsUri().replace("ws://", "http://");
    }

    @Override
    protected int managementPort(BackendTestResource backend) {
        return backend.getWsPort();
    }
}
