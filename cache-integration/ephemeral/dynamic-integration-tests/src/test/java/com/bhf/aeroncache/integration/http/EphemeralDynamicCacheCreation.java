package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;

@BackendTestConfig(dynamicCacheCreationEnabled = true, httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralDynamicCacheCreation extends DynamicCreateCacheTests {

    public EphemeralDynamicCacheCreation() {
        super(new CacheTestEndpoints());
    }
}
