package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamMultiCacheTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamMultiCacheTests extends AbstractMultiStreamMultiCacheTests<String> {

    public ClusteredMultiStreamMultiCacheTests() {
        super(new CacheTestEndpoints(), new WSStreamingHelper(new WSCacheTestEndpoints()), new SSEStreamingHelper(new SSECacheTestEndpoints()));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredMultiStreamMultiCacheTests-known-cache-1";
    }

    @Override
    protected String getAnotherKnownCacheId() {
        return "ClusteredMultiStreamMultiCacheTests-known-cache-2";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "ClusteredMultiStreamMultiCacheTests-known-value-2";
    }

    @Override
    protected String getKnownValue() {
        return "ClusteredMultiStreamMultiCacheTests-known-value-1";
    }
}
