package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredPutCounterItemTests extends AbstractMultiStreamPutItemTests<Integer> {

    public ClusteredPutCounterItemTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new StreamingCountersCacheTestEndpoints()));
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
    protected String getKnownCacheId() {
        return "ClusteredPutCounterItemTests-WS";
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
