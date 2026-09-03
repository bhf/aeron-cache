package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamBulkOpsCountersTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBulkOpsCountersTests extends AbstractMultiStreamBulkOpsCountersTests {

    public ClusteredBulkOpsCountersTests() {
        super(new CounterTestEndpoints(), new SSEStreamingHelper(new SSECountersCacheTestEndpoints()));
    }

}
