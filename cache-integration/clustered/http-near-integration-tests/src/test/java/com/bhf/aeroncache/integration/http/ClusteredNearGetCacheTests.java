package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.NearCacheTestEndpoints;

import java.util.Map;

@BackendTestConfig(httpEnabled = true, httpNearCacheEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredNearGetCacheTests extends GetCacheTests<String> {

    public ClusteredNearGetCacheTests() {
        super(new NearCacheTestEndpoints());
    }

    @Override
    Map<String, String> getKnownItems() {
        return Map.of(
                "Key1", "Value1",
                "Key2", "Value2",
                "Key3", "Value3"
        );
    }

    @Override
    String getHttpUri(BackendTestResource backend) {
        return backend.getBaseHttpNearUri();
    }

    @Override
    int getHttpPort(BackendTestResource backend) {
        return backend.getHttpNearPort();
    }
}
