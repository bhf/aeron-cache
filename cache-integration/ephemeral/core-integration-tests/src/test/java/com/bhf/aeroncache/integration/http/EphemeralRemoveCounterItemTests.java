package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralRemoveCounterItemTests extends RemoveItemTests<Long>{

    EphemeralRemoveCounterItemTests() {
        super(new CounterTestEndpoints());
    }

    @Override
    Long getKnownValue() {
        return 123L;
    }
}
