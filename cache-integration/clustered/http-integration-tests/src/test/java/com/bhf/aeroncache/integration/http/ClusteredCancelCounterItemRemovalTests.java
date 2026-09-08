package com.bhf.aeroncache.integration.http;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CounterTestEndpoints;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredCancelCounterItemRemovalTests extends CancelItemRemovalTests<Long> {

    ClusteredCancelCounterItemRemovalTests() {
        super(new CounterTestEndpoints());
    }

    @Override
    Long getKnownValue() {
        return 123L;
    }
}
