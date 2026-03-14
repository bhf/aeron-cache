package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractPutItemTests;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredPutItemTests extends AbstractPutItemTests {

    public ClusteredPutItemTests() {
        super(new SSEStreamingHelper());
    }
}
