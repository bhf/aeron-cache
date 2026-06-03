package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.*;

@BackendTestConfig(dynamicCacheCreationEnabled = true, httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamDynamicCacheCreationTests extends AbstractMultiStreamDynamicCacheTests {

    public ClusteredMultiStreamDynamicCacheCreationTests() {
        super(new WSStreamingHelper(), new SSEStreamingHelper());
    }

}
