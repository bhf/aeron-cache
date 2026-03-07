package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.clients.WSStreamingHelper;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractPutItemTests;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true)
class PutItemTests extends AbstractPutItemTests {

    public PutItemTests() {
        super(new WSStreamingHelper());
    }

}
