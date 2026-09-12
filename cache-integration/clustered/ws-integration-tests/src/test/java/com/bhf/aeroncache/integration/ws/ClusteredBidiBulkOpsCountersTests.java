package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamBulkOpsCountersTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredBulkOpsCountersTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiBulkOpsCountersTests extends AbstractMultiStreamBulkOpsCountersTests {

    public ClusteredBidiBulkOpsCountersTests() {
        super(new CounterTestEndpoints(), new BidiWsStreamingHelper(true));
    }
}
