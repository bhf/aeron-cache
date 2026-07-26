package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.NearCacheTestEndpoints;

@BackendTestConfig(httpEnabled = true, httpNearCacheEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredNearGetItemTests extends GetItemTests {

    public ClusteredNearGetItemTests() {
        super(new NearCacheTestEndpoints());
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
