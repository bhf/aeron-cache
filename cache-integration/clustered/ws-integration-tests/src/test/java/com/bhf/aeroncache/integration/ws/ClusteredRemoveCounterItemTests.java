package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamRemoveItemTest;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredRemoveCounterItemTests extends AbstractMultiStreamRemoveItemTest<Integer> {

    public ClusteredRemoveCounterItemTests() {
        super(new CounterTestEndpoints(), new StreamingCountersCacheTestEndpoints(), new WSStreamingHelper());
    }

    @Override
    public Integer getKnownValue() {
        return 123;
    }
}
