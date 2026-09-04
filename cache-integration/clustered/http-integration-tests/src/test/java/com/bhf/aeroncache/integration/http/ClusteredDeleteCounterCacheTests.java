package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredDeleteCounterCacheTests extends DeleteCacheTests {

    ClusteredDeleteCounterCacheTests() {
        super(new CounterTestEndpoints());
    }
}
