package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamKeySubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralSSEKeyCounterSubscriptionTests extends AbstractMultiStreamKeySubscriptionTests<Integer> {

    public EphemeralSSEKeyCounterSubscriptionTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralSSEKeyCounterSubscriptionTestsCache";
    }

    @Override
    protected Integer getSubscribedValue() {
        return 1;
    }

    @Override
    protected Integer getOtherValue() {
        return 2;
    }
}
