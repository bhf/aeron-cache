package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

import java.util.Map;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredGetCounterCacheTests extends GetCacheTests<Integer> {

    ClusteredGetCounterCacheTests() {
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
