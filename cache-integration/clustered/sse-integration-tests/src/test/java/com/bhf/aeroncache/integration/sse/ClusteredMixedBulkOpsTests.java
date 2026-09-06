package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMixedBulkOpsTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.StreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMixedBulkOpsTests extends AbstractMultiStreamMixedBulkOpsTests {

    public ClusteredMixedBulkOpsTests() {
        super(new CacheTestEndpoints(), new CounterTestEndpoints(),
                new StreamingHelper[]{new SSEStreamingHelper(new SSECacheTestEndpoints())},
                new StreamingHelper[]{new SSEStreamingHelper(new SSECountersCacheTestEndpoints())});
    }

}
