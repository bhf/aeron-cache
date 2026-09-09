package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPatchSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralMultiStreamPatchSubscriptionTests extends AbstractMultiStreamPatchSubscriptionTests {

    public EphemeralMultiStreamPatchSubscriptionTests() {
        super(new CacheTestEndpoints(), new SSEStreamingHelper(new SSECacheTestEndpoints()), new WSStreamingHelper(new WSCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "EphemeralMultiStreamPatchSubscriptionTestsCache";
    }
}
