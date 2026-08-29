package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.SSECacheTestEndpoints;
import com.bhf.aeroncache.integration.config.WSCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;
import com.bhf.aeroncache.integration.streaming.MultiStreamRestartTests;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
public class ClusteredMultiStreamRestartTests extends MultiStreamRestartTests<String> {
    public ClusteredMultiStreamRestartTests() {
        super(new SSEStreamingHelper(new SSECacheTestEndpoints()), new WSStreamingHelper(new WSCacheTestEndpoints()),
                new CacheTestEndpoints());
    }

    @Override
    protected String getKnownValue() {
        return "ClusteredMultiStreamRestartTests";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "ClusteredMultiStreamRestartTests-anotherValue";
    }
}
