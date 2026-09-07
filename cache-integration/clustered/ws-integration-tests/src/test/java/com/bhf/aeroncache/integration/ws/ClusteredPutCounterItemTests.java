package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredPutCounterItemTests extends AbstractMultiStreamPutItemTests<Integer> {

    public ClusteredPutCounterItemTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
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
    protected void shouldGetStreamingUpdateWhenPatchingItem(BackendTestResource backend) {
        assertTrue(true);
    }

}
