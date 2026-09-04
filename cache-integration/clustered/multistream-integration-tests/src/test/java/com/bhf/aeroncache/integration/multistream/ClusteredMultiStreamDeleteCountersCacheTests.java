package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamDeleteCacheTest;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamDeleteCountersCacheTests extends AbstractMultiStreamDeleteCacheTest<Integer> {

    public ClusteredMultiStreamDeleteCountersCacheTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()),
                new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiStreamDeleteCountersCacheTests";
    }

    @Override
    public Integer getKnownValue() {
        return 123;
    }
}
