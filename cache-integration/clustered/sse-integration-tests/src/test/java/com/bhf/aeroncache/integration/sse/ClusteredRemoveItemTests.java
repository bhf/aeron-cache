package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamRemoveItemTest;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredRemoveItemTests extends AbstractMultiStreamRemoveItemTest {

    public ClusteredRemoveItemTests() {
        super(new CacheTestEndpoints(), new SSEStreamingHelper());
    }
}
