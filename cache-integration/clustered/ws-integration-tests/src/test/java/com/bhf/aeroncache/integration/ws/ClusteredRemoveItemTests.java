package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamRemoveItemTest;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredRemoveItemTests extends AbstractMultiStreamRemoveItemTest {

    public ClusteredRemoveItemTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper());
    }

}
