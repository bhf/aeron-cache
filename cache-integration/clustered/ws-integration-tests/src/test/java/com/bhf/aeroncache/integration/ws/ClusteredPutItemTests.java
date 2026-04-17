package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractPutItemTests;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredPutItemTests extends AbstractMultiStreamPutItemTests {

    public ClusteredPutItemTests() {
        super(new WSStreamingHelper());
    }

}
