package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamKeySubscriptionTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredWSKeyCounterSubscriptionTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiKeyCounterSubscriptionTests extends AbstractMultiStreamKeySubscriptionTests<Integer> {

    public ClusteredBidiKeyCounterSubscriptionTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiKeyCounterSubscriptionTestsCache";
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
