package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredPutItemTests extends AbstractMultiStreamPutItemTests<String> {

    public ClusteredPutItemTests() {
        super(new CacheTestEndpoints(), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredPutItemTestsCache-SSE";
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
