package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPatchSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamPatchSubscriptionTests extends AbstractMultiStreamPatchSubscriptionTests {

    public ClusteredMultiStreamPatchSubscriptionTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new WSCacheTestEndpoints()), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiStreamPatchSubscriptionTestsCache";
    }
}
