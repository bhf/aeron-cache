package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.*;

@BackendTestConfig(dynamicCacheCreationEnabled = true, httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamDynamicCacheCreationTests extends AbstractMultiStreamDynamicCacheTests<String> {

    public ClusteredMultiStreamDynamicCacheCreationTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new StreamingCacheTestEndpoints()), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getKnownValue() {
        return "ClusteredMultiStreamDynamicCacheCreationTests";
    }
}
