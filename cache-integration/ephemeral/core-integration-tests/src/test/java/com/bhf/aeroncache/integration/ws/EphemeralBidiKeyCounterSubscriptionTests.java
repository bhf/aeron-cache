package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamKeySubscriptionTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** Unclustered BIDI-endpoint variant of {@link EphemeralWSKeyCounterSubscriptionTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiKeyCounterSubscriptionTests extends AbstractMultiStreamKeySubscriptionTests<Integer> {

    public EphemeralBidiKeyCounterSubscriptionTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralBidiKeyCounterSubscriptionTestsCache";
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
