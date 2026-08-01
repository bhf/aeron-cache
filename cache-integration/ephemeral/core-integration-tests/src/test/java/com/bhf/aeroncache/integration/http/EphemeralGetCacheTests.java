package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;

import java.util.Map;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralGetCacheTests extends GetCacheTests<String> {

    EphemeralGetCacheTests() {
        super(new CacheTestEndpoints());
    }

    @Override
    Map<String, String> getKnownItems() {
        return Map.of(
                "Key1", "Value1",
                "Key2", "Value2",
                "Key3", "Value3"
        );
    }
}
