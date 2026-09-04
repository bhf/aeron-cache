package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamMultiSubscriptionTests extends AbstractMultiStreamMultiSubscriptionTests<String> {

    public ClusteredMultiStreamMultiSubscriptionTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new WSCacheTestEndpoints()), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiStreamMultiSubscriptionTestsCache1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredMultiStreamMultiSubscriptionTestsCache2";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "SomeOtherValue";
    }

    @Override
    protected String getKnownValue() {
        return "SomeValue";
    }
}
