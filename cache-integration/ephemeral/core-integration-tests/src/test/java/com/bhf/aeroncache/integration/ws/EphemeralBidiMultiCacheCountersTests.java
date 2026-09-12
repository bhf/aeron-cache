package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** Unclustered BIDI-endpoint variant of {@link EphemeralWSMultiCacheCountersTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiMultiCacheCountersTests extends AbstractMultiStreamMultiCacheTests<Integer> {

    public EphemeralBidiMultiCacheCountersTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralBidiMultiCacheCountersTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralBidiMultiCacheCountersTestsCache2";
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
