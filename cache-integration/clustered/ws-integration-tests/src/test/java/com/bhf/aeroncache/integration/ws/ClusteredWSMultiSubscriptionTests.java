package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredWSMultiSubscriptionTests extends AbstractMultiStreamMultiSubscriptionTests<String> {

    public ClusteredWSMultiSubscriptionTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new WSCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredWSMultiSubscriptionTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredWSMultiSubscriptionTestsCache2";
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
