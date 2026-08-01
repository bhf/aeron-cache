package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralPutCounterItemTests extends PutItemTests<Integer> {

    EphemeralPutCounterItemTests() {
        super(new CounterTestEndpoints());
    }

    @Override
    Integer getKnownValue() {
        return 123;
    }
}
