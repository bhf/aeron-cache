package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamDecrementCounterTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredDecrementCounterTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiDecrementCounterTests extends AbstractMultiStreamDecrementCounterTests {

    public ClusteredBidiDecrementCounterTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiDecrementCounterTests-BIDI";
    }
}
