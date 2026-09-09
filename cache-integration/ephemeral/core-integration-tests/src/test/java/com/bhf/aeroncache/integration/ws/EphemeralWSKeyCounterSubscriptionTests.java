package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamKeySubscriptionTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralWSKeyCounterSubscriptionTests extends AbstractMultiStreamKeySubscriptionTests<Integer> {

    public EphemeralWSKeyCounterSubscriptionTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralWSKeyCounterSubscriptionTestsCache";
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
