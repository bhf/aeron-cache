package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralGetItemTests extends GetItemTests<String> {

    EphemeralGetItemTests() {
        super(new CacheTestEndpoints());
    }

    @Override
    String getKnownValue() {
        return "SomeValue";
    }

    @Override
    String getNotFoundValue() {
        return "";
    }

    @Override
    String getUnknownCacheValue() {
        return "NA";
    }
}
