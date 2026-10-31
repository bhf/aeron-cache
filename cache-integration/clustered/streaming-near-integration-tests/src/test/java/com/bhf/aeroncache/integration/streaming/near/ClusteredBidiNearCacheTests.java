package com.bhf.aeroncache.integration.streaming.near;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractStreamingNearCacheTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/**
 * Near cache integration tests driven over the bidirectional websocket endpoint ({@code /api/ws/v1/bidi}).
 * The bidi subscribe path shares the WS interface's near cache (the same subscription publishers back both
 * the one-directional routes and the bidi endpoint), so a whole-cache hydrating bidi subscribe warms and is
 * served from the near cache exactly as the one-directional routes are. Mutations go over HTTP; updates
 * arrive over the bidi socket.
 */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true,
        useTestContainersEnvironment = true, streamingNearCacheEnabled = true)
class ClusteredBidiNearCacheTests extends AbstractStreamingNearCacheTests {

    public ClusteredBidiNearCacheTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper());
    }

    @Override
    protected String getCacheId() {
        return "clustered-near-cache-bidi";
    }

    @Override
    protected String managementBaseUri(BackendTestResource backend) {
        // The bidi endpoint lives on the WS interface, which serves its near cache management endpoints
        // over HTTP on the same host/port.
        return backend.getBaseWsUri().replace("ws://", "http://");
    }

    @Override
    protected int managementPort(BackendTestResource backend) {
        return backend.getWsPort();
    }
}
