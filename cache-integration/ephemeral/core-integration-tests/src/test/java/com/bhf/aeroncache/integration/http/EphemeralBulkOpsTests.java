package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralBulkOpsTests extends BulkItemsTests {

    EphemeralBulkOpsTests() {
        super(new CacheTestEndpoints());
    }
}
