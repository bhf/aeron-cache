package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredMultiCountersCacheTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiMultiCountersCacheTests extends AbstractMultiStreamMultiCacheTests<Integer> {

    public ClusteredBidiMultiCountersCacheTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiMultiCountersCacheTests-known-cache-1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredBidiMultiCountersCacheTests-known-cache-2";
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
