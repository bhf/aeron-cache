package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** Unclustered BIDI-endpoint variant of {@link EphemeralWSMultiCacheTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiMultiCacheTests extends AbstractMultiStreamMultiCacheTests<String> {

    public EphemeralBidiMultiCacheTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralBidiMultiCacheTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralBidiMultiCacheTestsCache2";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "AnotherKnownValue";
    }

    @Override
    protected String getKnownValue() {
        return "KnownValue";
    }
}
