package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralRemoveItemTests extends RemoveItemTests<String>{

    EphemeralRemoveItemTests() {
        super(new CacheTestEndpoints());
    }

    @Override
    String getKnownValue() {
        return "knownValue";
    }
}
