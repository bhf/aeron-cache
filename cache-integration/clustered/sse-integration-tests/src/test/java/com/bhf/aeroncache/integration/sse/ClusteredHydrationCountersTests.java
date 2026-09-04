package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamHydrationTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredHydrationCountersTests extends AbstractMultiStreamHydrationTests<Integer> {

    public ClusteredHydrationCountersTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "clustered-hydration-test-counters-sse";
    }

    @Override
    protected Integer getHydrationValue2() {
        return 2;
    }

    @Override
    protected Integer getHydrationValue1() {
        return 1;
    }

}
