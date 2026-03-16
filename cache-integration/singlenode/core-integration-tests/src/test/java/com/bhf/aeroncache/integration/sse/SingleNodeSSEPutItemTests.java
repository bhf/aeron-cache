package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractPutItemTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class SingleNodeSSEPutItemTests extends AbstractPutItemTests {

    public SingleNodeSSEPutItemTests() {
        super(new SSEStreamingHelper());
    }
}
