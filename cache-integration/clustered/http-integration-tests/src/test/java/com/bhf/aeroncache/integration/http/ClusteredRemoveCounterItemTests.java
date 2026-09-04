package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredRemoveCounterItemTests extends RemoveItemTests<Long>{

    ClusteredRemoveCounterItemTests() {
        super(new CounterTestEndpoints());
    }

    @Override
    Long getKnownValue() {
        return 123L;
    }
}
