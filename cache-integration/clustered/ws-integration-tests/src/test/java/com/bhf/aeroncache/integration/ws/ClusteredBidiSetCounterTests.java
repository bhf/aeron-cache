package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamSetCounterTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredSetCounterTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiSetCounterTests extends AbstractMultiStreamSetCounterTests {

    public ClusteredBidiSetCounterTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiSetCounterTests-BIDI";
    }
}
