package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamDynamicCacheTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(dynamicCacheCreationEnabled = true, httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamDynamicCountersCacheCreationTests extends AbstractMultiStreamDynamicCacheTests<Integer> {

    public ClusteredMultiStreamDynamicCountersCacheCreationTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()),
                new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

    @Override
    protected Integer getKnownValue() {
        return 1;
    }
}
