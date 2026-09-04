package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralMultiStreamMultiSubscriptionTests extends AbstractMultiStreamMultiSubscriptionTests<String> {

    public EphemeralMultiStreamMultiSubscriptionTests() {
        super(new CacheTestEndpoints(), new SSEStreamingHelper(new SSECacheTestEndpoints()), new WSStreamingHelper(new WSCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralMultiStreamMultiSubscriptionTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "EphemeralMultiStreamMultiSubscriptionTestsCache2";
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
