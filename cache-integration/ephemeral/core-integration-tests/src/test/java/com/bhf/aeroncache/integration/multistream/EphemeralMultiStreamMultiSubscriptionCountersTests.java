package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralMultiStreamMultiSubscriptionCountersTests extends AbstractMultiStreamMultiSubscriptionTests<Integer> {

    public EphemeralMultiStreamMultiSubscriptionCountersTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()), new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralMultiStreamMultiSubscriptionCountersTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralMultiStreamMultiSubscriptionCountersTestsCache2";
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
