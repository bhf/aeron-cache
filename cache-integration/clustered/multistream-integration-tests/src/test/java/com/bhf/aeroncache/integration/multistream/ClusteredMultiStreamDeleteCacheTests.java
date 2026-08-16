package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamDeleteCacheTest;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamDeleteCacheTests extends AbstractMultiStreamDeleteCacheTest<String> {

    public ClusteredMultiStreamDeleteCacheTests() {
        super(new CacheTestEndpoints(), new StreamingCacheTestEndpoints(), new WSStreamingHelper(), new SSEStreamingHelper());
    }

    @Override
    public String getKnownValue() {
        return "ClusteredMultiStreamDeleteCacheTests";
    }
}
