package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCountersCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMixedBulkOpsTests;
import com.bhf.aeroncache.integration.streaming.StreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMixedBulkOpsTests extends AbstractMultiStreamMixedBulkOpsTests {

    public ClusteredMixedBulkOpsTests() {
        super(new CacheTestEndpoints(), new CounterTestEndpoints(),
                new StreamingHelper[]{new WSStreamingHelper(new WSCacheTestEndpoints())},
                new StreamingHelper[]{new WSStreamingHelper(new WSCountersCacheTestEndpoints())});
    }

}
