package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** BIDI-endpoint variant of {@link ClusteredPutCounterItemTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiPutCounterItemTests extends AbstractMultiStreamPutItemTests<Integer> {

    public ClusteredBidiPutCounterItemTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected Integer getKnownValue() {
        return 1;
    }

    @Override
    protected Integer getAnotherKnownValue() {
        return 2;
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiPutCounterItemTests-BIDI";
    }

    @Override
    protected void shouldGetStreamingUpdateWhenPatchingItem(BackendTestResource backend) {
        // Patch is not applicable to counters; skip as the WS counter variant does.
        assertTrue(true);
    }
}
