package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
public class ClusteredMultiStreamRestartTests extends MultiStreamRestartTests {
    protected ClusteredMultiStreamRestartTests() {
        super(new SSEStreamingHelper(), new WSStreamingHelper());
    }
}
