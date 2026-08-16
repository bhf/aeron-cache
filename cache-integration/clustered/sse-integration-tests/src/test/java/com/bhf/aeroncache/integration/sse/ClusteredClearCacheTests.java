package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamClearCacheTest;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredClearCacheTests extends AbstractMultiStreamClearCacheTest<String> {

    public ClusteredClearCacheTests() {
        super(new CacheTestEndpoints(), new StreamingCacheTestEndpoints(), new SSEStreamingHelper());
    }

    @Override
    public String getKnownValue() {
        return "ClusteredClearCacheTests";
    }
}
