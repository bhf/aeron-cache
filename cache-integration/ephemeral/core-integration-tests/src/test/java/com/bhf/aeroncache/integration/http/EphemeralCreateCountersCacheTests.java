package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralCreateCountersCacheTests extends CreateCacheTests {

    EphemeralCreateCountersCacheTests() {
        super("/api/v1/counters/", new CounterTestEndpoints());
    }
}
