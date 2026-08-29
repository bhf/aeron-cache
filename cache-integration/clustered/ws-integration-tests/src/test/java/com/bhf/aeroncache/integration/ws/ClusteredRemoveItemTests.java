package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamRemoveItemTest;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredRemoveItemTests extends AbstractMultiStreamRemoveItemTest<String> {

    public ClusteredRemoveItemTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new StreamingCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredRemoveItemTests-WS";
    }

    @Override
    public String getKnownValue() {
        return "ClusteredRemoveItemTests";
    }
}
