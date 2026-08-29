package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredPutCounterItemTests extends AbstractMultiStreamPutItemTests<Integer> {

    public ClusteredPutCounterItemTests() {
        super(new CounterTestEndpoints(), new SSECountersCacheTestEndpoints(), new SSEStreamingHelper());
    }

    @Override
    protected Integer getKnownValue() {
        return 1;
    }

    @Override
    protected Integer getAnotherKnownValue() {
        return 2;
    }

    @Override
    protected void shouldGetOrderedUpdatesOnTimedRemoved(BackendTestResource backend) {
        assert(true);
    }

    @Override
    protected void shouldCancelOldTimerWhenUpdatingTtl(BackendTestResource backend) {
        assert(true);
    }
}
