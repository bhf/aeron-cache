package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredGetCacheTests extends GetCacheTests {

    ClusteredGetCacheTests() {
        super("/api/v1/cache/");
    }
}
