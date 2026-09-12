package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractBidiCommandTests;

/** Unclustered (ephemeral) variant of the command-over-socket BIDI suite. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiCommandTests extends AbstractBidiCommandTests {

    @Override
    protected String cacheIdPrefix() {
        return "EphemeralBidiCommandTests";
    }
}
