package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralMultiStreamMultiCacheCountersTests extends AbstractMultiStreamMultiCacheTests<Integer> {

    public EphemeralMultiStreamMultiCacheCountersTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()),
                new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralMultiStreamMultiCacheCountersTests-known-cache-1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralMultiStreamMultiCacheCountersTests-known-cache-2";
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
