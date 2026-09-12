package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** Unclustered BIDI-endpoint variant of {@link EphemeralWSMultiSubscriptionCountersTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiMultiSubscriptionCountersTests extends AbstractMultiStreamMultiSubscriptionTests<Integer> {

    public EphemeralBidiMultiSubscriptionCountersTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralBidiMultiSubscriptionCountersTestsCache";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralBidiMultiSubscriptionCountersTestsAnotherCache";
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
