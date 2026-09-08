package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralCancelCounterItemRemovalTests extends CancelItemRemovalTests<Long> {

    EphemeralCancelCounterItemRemovalTests() {
        super(new CounterTestEndpoints());
    }

    @Override
    Long getKnownValue() {
        return 123L;
    }
}
