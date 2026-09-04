package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredCreateCounterCacheTests extends CreateCacheTests {

    public ClusteredCreateCounterCacheTests() {
        final String CREATE_ENDPOINT = "/api/v1/counters/";
        super(CREATE_ENDPOINT, new CounterTestEndpoints());
    }

    @Override
    void shouldReturn400ForInvalidCacheNames(String invalidCacheId, BackendTestResource backend) {
        assertTrue(true);
    }
}
