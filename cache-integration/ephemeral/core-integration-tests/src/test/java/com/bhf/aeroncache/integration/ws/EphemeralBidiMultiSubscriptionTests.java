package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** Unclustered BIDI-endpoint variant of {@link EphemeralWSMultiSubscriptionTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiMultiSubscriptionTests extends AbstractMultiStreamMultiSubscriptionTests<String> {

    public EphemeralBidiMultiSubscriptionTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralBidiMultiSubscriptionTestsCache";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralBidiMultiSubscriptionTestsAnotherCache";
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
