package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralGetCounterItemTests extends GetItemTests<Integer> {

    EphemeralGetCounterItemTests() {
        super(new CounterTestEndpoints());
    }

    @Override
    Integer getKnownValue() {
        return 123;
    }

    @Override
    Integer getNotFoundValue() {
        return 0;
    }

    @Override
    Integer getUnknownCacheValue() {
        return 0;
    }
}
