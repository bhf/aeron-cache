package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractPutItemTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** Unclustered BIDI-endpoint variant of {@link EphemeralWSPutItemTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiPutItemTests extends AbstractPutItemTests {

    public EphemeralBidiPutItemTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }
}
