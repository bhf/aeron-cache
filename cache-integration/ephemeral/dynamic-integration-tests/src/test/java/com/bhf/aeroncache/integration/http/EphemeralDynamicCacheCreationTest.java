package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;

@BackendTestConfig(dynamicCacheCreationEnabled = true, httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralDynamicCacheCreationTest extends DynamicCreateCacheTests {

    public EphemeralDynamicCacheCreationTest() {
        super(new CacheTestEndpoints());
    }
}
