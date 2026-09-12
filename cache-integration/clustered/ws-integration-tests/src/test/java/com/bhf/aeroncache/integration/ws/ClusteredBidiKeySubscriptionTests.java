package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamKeySubscriptionTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/** BIDI-endpoint variant of {@link ClusteredWSKeySubscriptionTests}. */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiKeySubscriptionTests extends AbstractMultiStreamKeySubscriptionTests<String> {

    public ClusteredBidiKeySubscriptionTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiKeySubscriptionTestsCache";
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
