package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMixedBulkOpsTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;
import com.bhf.aeroncache.integration.streaming.StreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredMixedBulkOpsTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiMixedBulkOpsTests extends AbstractMultiStreamMixedBulkOpsTests {

    public ClusteredBidiMixedBulkOpsTests() {
        super(new CacheTestEndpoints(), new CounterTestEndpoints(),
                new StreamingHelper[]{new BidiWsStreamingHelper(false)},
                new StreamingHelper[]{new BidiWsStreamingHelper(true)});
    }
}
