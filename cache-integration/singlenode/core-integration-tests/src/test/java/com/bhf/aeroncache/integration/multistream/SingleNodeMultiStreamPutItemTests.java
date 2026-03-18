package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class SingleNodeMultiStreamPutItemTests extends AbstractMultiStreamPutItemTests {

    public SingleNodeMultiStreamPutItemTests() {
        super(new SSEStreamingHelper(), new WSStreamingHelper());
    }
}
