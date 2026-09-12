package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredMultiCacheTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiMultiCacheTests extends AbstractMultiStreamMultiCacheTests<String> {

    public ClusteredBidiMultiCacheTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiMultiCacheTests-known-cache-1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredBidiMultiCacheTests-known-cache-2";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "ClusteredBidiMultiCacheTests-known-value-2";
    }

    @Override
    protected String getKnownValue() {
        return "ClusteredBidiMultiCacheTests-known-value-1";
    }
}
