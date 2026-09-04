package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredGetItemTests extends GetItemTests<String> {

    ClusteredGetItemTests() {
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
