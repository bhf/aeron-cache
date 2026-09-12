package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamHydrationTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredHydrationCountersTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiHydrationCountersTests extends AbstractMultiStreamHydrationTests<Integer> {

    public ClusteredBidiHydrationCountersTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "clustered-bidi-hydration-test-counters";
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
