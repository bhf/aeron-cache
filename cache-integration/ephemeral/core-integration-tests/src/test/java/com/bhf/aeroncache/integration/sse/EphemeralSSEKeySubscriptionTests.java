package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamKeySubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralSSEKeySubscriptionTests extends AbstractMultiStreamKeySubscriptionTests<String> {

    public EphemeralSSEKeySubscriptionTests() {
        super(new CacheTestEndpoints(), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralSSEKeySubscriptionTestsCache";
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
