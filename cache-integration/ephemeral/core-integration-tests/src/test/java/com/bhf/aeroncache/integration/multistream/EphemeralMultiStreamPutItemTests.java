package com.bhf.aeroncache.integration.multistream;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.StreamingCacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class EphemeralMultiStreamPutItemTests extends AbstractMultiStreamPutItemTests<String> {

    public EphemeralMultiStreamPutItemTests() {
        super(new CacheTestEndpoints(), new StreamingCacheTestEndpoints(), new SSEStreamingHelper(), new WSStreamingHelper());
    }

    @Override
    protected void shouldGetOrderedUpdatesOnTimedRemoved(BackendTestResource backend) {
        // not supported for ephemeral caches
        assertTrue(true);
    }

    @Override
    protected void shouldCancelOldTimerWhenUpdatingTtl(BackendTestResource backend) {
        // not supported for ephemeral caches
        assertTrue(true);
    }

    @Override
    protected String getKnownValue() {
        return "EphemeralMultiStreamPutItemTests";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "EphemeralMultiStreamPutItemTests-second-value";
    }
}
