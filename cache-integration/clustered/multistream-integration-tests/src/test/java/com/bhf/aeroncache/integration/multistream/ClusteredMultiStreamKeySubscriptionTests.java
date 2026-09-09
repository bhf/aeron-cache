package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamKeySubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamKeySubscriptionTests extends AbstractMultiStreamKeySubscriptionTests<String> {

    public ClusteredMultiStreamKeySubscriptionTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new WSCacheTestEndpoints()), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiStreamKeySubscriptionTestsCache";
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
