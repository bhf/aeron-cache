package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredCreateCacheTests extends CreateCacheTests {

    private static final String CREATE_ENDPOINT = "/api/v1/cache/";

    public ClusteredCreateCacheTests() {
        super(CREATE_ENDPOINT, new CacheTestEndpoints());
    }
}
