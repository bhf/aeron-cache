package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredMultiStreamPutItemTests extends AbstractMultiStreamPutItemTests<String> {

    public ClusteredMultiStreamPutItemTests() {
        super(new CacheTestEndpoints(), new StreamingCacheTestEndpoints(), new WSStreamingHelper(), new SSEStreamingHelper());
    }

    @Override
    protected String getKnownValue() {
        return "ClusteredMultiStreamPutItemTests";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "ClusteredMultiStreamPutItemTests-second-value";
    }
}
