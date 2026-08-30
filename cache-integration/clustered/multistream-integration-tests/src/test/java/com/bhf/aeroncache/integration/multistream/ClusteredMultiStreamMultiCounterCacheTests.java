package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamMultiCounterCacheTests extends AbstractMultiStreamMultiCacheTests<Integer> {

    public ClusteredMultiStreamMultiCounterCacheTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()),
                new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiStreamMultiCacheTests-known-cache-1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredMultiStreamMultiCacheTests-known-cache-2";
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
