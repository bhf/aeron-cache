package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamClearCacheTest;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamRemoveItemTest;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamClearCacheTests extends AbstractMultiStreamClearCacheTest {

    public ClusteredMultiStreamClearCacheTests() {
        super(new CacheTestEndpoints(), new StreamingCacheTestEndpoints(), new WSStreamingHelper(), new SSEStreamingHelper());
    }

}
