package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiCountersCacheTests extends AbstractMultiStreamMultiCacheTests<Integer> {

    public ClusteredMultiCountersCacheTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiCacheTests-known-cache-1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredMultiCacheTests-known-cache-2";
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
