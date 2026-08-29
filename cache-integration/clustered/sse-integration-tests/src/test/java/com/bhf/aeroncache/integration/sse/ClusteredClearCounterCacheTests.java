package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamClearCacheTest;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredClearCounterCacheTests extends AbstractMultiStreamClearCacheTest<Integer> {

    public ClusteredClearCounterCacheTests() {
        super(new CounterTestEndpoints(), new SSECountersCacheTestEndpoints(), new SSEStreamingHelper());
    }

    @Override
    public Integer getKnownValue() {
        return 123;
    }
}
