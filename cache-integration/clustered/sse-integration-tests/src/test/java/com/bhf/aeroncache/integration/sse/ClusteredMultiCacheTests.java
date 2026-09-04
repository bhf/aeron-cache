package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiCacheTests extends AbstractMultiStreamMultiCacheTests<String> {

    public ClusteredMultiCacheTests() {
        super(new CacheTestEndpoints(), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiCacheTests-sse-1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredMultiCacheTests-sse-2";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "ClusteredMultiCacheTests-value-2";
    }

    @Override
    protected String getKnownValue() {
        return "ClusteredMultiCacheTests-value-1";
    }
}
