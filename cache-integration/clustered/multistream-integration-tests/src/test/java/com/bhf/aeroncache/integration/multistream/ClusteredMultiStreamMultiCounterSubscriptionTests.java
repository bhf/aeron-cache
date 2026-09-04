package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamMultiCounterSubscriptionTests extends AbstractMultiStreamMultiSubscriptionTests<Integer> {

    public ClusteredMultiStreamMultiCounterSubscriptionTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()),
                new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiStreamMultiCounterSubscriptionTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredMultiStreamMultiCounterSubscriptionTestsCache2";
    }

    @Override
    protected Integer getAnotherKnownValue() {
        return 0;
    }

    @Override
    protected Integer getKnownValue() {
        return 0;
    }

}
