package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamDynamicCacheTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(dynamicCacheCreationEnabled = true, httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralMultiStreamDynamicCacheCreationTests extends AbstractMultiStreamDynamicCacheTests {

    public EphemeralMultiStreamDynamicCacheCreationTests() {
        super(new WSStreamingHelper(), new SSEStreamingHelper());
    }

}
