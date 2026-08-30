package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralWSMultiSubscriptionTests extends AbstractMultiStreamMultiSubscriptionTests<String> {

    public EphemeralWSMultiSubscriptionTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new WSCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralWSMultiSubscriptionTestsCache";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralWSMultiSubscriptionTestsAnotherCache";
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
