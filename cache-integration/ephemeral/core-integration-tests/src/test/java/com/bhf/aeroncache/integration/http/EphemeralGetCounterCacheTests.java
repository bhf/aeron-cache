package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

import java.util.Map;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralGetCounterCacheTests extends GetCacheTests<Integer> {

    EphemeralGetCounterCacheTests() {
        super(new CounterTestEndpoints());
    }

    @Override
    Map<String, Integer> getKnownItems() {
        return Map.of(
                "Key1", 1,
                "Key2", 2,
                "Key3", 3
        );
    }
}
