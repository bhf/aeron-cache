package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamBulkOpsCountersTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBulkOpsCountersTests extends AbstractMultiStreamBulkOpsCountersTests {

    public ClusteredBulkOpsCountersTests() {
        super(new CounterTestEndpoints(), new WSStreamingHelper(new WSCountersCacheTestEndpoints()));
    }

}
