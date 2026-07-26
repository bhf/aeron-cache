package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiSubscriptionTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralMultiStreamMultiSubscriptionTests extends AbstractMultiStreamMultiSubscriptionTests {

    public EphemeralMultiStreamMultiSubscriptionTests() {
        super(new CacheTestEndpoints(), new SSEStreamingHelper(), new WSStreamingHelper());
    }
}
