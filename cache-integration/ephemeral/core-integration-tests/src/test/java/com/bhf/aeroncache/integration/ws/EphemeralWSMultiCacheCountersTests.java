package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralWSMultiCacheCountersTests extends AbstractMultiStreamMultiCacheTests<Integer> {

    public EphemeralWSMultiCacheCountersTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralWSMultiCacheCountersTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralWSMultiCacheCountersTestsCache2";
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
