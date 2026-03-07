package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.clients.WSStreamingHelper;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractPutItemTests;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true)
class ClusteredPutItemTests extends AbstractPutItemTests {

    public ClusteredPutItemTests() {
        super(new WSStreamingHelper());
    }

}
