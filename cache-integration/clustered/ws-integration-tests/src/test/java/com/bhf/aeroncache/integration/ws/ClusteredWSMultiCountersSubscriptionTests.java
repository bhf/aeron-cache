package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredWSMultiCountersSubscriptionTests extends AbstractMultiStreamMultiSubscriptionTests<Integer> {

    public ClusteredWSMultiCountersSubscriptionTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredWSMultiCounterSubscriptionTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredWSMultiCounterSubscriptionTestsCache2";
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
