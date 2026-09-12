package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamKeySubscriptionTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** Unclustered BIDI-endpoint variant of {@link EphemeralWSKeySubscriptionTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBidiKeySubscriptionTests extends AbstractMultiStreamKeySubscriptionTests<String> {

    public EphemeralBidiKeySubscriptionTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralBidiKeySubscriptionTestsCache";
    }

    @Override
    protected String getSubscribedValue() {
        return "SubscribedValue";
    }

    @Override
    protected String getOtherValue() {
        return "OtherValue";
    }
}
