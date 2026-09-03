package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralSSEMultiSubscriptionCountersTests extends AbstractMultiStreamMultiSubscriptionTests<Integer> {

    public EphemeralSSEMultiSubscriptionCountersTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralSSEMultiSubscriptionCountersTestsCache";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralSSEMultiSubscriptionCountersTestsAnotherCache";
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
