package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamBulkOpsTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredBulkOpsTests}: streaming observed over /api/ws/v1/bidi. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiBulkOpsTests extends AbstractMultiStreamBulkOpsTests {

    public ClusteredBidiBulkOpsTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }
}
