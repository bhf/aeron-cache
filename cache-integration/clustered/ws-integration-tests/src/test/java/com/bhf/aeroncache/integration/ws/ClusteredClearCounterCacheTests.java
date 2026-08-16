package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamClearCacheTest;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredClearCounterCacheTests extends AbstractMultiStreamClearCacheTest<Integer> {

    public ClusteredClearCounterCacheTests() {
        super(new CounterTestEndpoints(), new StreamingCountersCacheTestEndpoints(), new WSStreamingHelper());
    }

    @Override
    public Integer getKnownValue() {
        return 123;
    }
}
