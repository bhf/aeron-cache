package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractBidiCommandTests;

/** Clustered variant of the command-over-socket BIDI suite. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiCommandTests extends AbstractBidiCommandTests {

    @Override
    protected String cacheIdPrefix() {
        return "ClusteredBidiCommandTests";
    }
}
