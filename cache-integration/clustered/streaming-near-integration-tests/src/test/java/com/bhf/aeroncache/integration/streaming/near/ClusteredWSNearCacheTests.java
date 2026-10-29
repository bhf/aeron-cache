package com.bhf.aeroncache.integration.streaming.near;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractStreamingNearCacheTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true,
        useTestContainersEnvironment = true, streamingNearCacheEnabled = true)
class ClusteredWSNearCacheTests extends AbstractStreamingNearCacheTests {

    public ClusteredWSNearCacheTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new WSCacheTestEndpoints()));
    }

    @Override
    protected String getCacheId() {
        return "clustered-near-cache-ws";
    }

    @Override
    protected String managementBaseUri(BackendTestResource backend) {
        // The WS interface serves its near cache management endpoints over HTTP on the same host/port.
        return backend.getBaseWsUri().replace("ws://", "http://");
    }

    @Override
    protected int managementPort(BackendTestResource backend) {
        return backend.getWsPort();
    }
}
