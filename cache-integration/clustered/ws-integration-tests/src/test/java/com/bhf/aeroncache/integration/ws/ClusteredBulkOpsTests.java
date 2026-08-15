package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamBulkOpsTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBulkOpsTests extends AbstractMultiStreamBulkOpsTests {

    public ClusteredBulkOpsTests() {
        super(new CacheTestEndpoints(), new StreamingCacheTestEndpoints(), new WSStreamingHelper());
    }

}
