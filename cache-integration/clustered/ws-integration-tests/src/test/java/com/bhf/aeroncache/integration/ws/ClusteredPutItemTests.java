package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredPutItemTests extends AbstractMultiStreamPutItemTests<String> {

    public ClusteredPutItemTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new WSCacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredPutItemTests-WS";
    }

    @Override
    protected String getKnownValue() {
        return "ClusteredPutItemTests";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "ClusteredPutItemTests-second-value";
    }
}
