package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamDeleteCacheTest;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredDeleteCounterCacheTests extends AbstractMultiStreamDeleteCacheTest<Integer> {

    public ClusteredDeleteCounterCacheTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new StreamingCountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredDeleteCounterCacheTests-WS";
    }

    @Override
    public Integer getKnownValue() {
        return 456;
    }
}
