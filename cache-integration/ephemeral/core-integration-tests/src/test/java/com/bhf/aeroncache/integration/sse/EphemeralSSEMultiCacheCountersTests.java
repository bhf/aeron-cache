package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralSSEMultiCacheCountersTests extends AbstractMultiStreamMultiCacheTests<Integer> {

    public EphemeralSSEMultiCacheCountersTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralSSEMultiCacheCountersTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralSSEMultiCacheCountersTestsCache2";
    }

    @Override
    protected Integer getAnotherKnownValue() {
        return 2;
    }

    @Override
    protected Integer getKnownValue() {
        return 1;
    }
}
