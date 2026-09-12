package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamIncrementCounterTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredIncrementCounterTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiIncrementCounterTests extends AbstractMultiStreamIncrementCounterTests {

    public ClusteredBidiIncrementCounterTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiIncrementCounterTests-BIDI";
    }
}
