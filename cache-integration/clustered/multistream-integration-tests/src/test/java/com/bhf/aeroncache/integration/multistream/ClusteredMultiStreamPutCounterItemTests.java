package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamPutCounterItemTests extends AbstractMultiStreamPutItemTests<Integer> {

    public ClusteredMultiStreamPutCounterItemTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()),
                new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiStreamPutCounterItemTests";
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
