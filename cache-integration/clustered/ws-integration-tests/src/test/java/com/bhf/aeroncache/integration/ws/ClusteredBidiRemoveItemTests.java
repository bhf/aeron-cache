package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamRemoveItemTest;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredRemoveItemTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiRemoveItemTests extends AbstractMultiStreamRemoveItemTest<String> {

    public ClusteredBidiRemoveItemTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiRemoveItemTests-BIDI";
    }

    @Override
    public String getKnownValue() {
        return "ClusteredBidiRemoveItemTests";
    }
}
