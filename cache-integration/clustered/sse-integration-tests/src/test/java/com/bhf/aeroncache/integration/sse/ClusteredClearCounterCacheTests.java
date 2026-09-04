package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamClearCacheTest;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredClearCounterCacheTests extends AbstractMultiStreamClearCacheTest<Integer> {

    public ClusteredClearCounterCacheTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredClearCounterCacheTests-SSE";
    }

    @Override
    public Integer getKnownValue() {
        return 123;
    }
}
