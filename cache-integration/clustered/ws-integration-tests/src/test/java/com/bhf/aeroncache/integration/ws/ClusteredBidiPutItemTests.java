package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.streaming.AbstractMultiStreamPutItemTests;
import com.bhf.aeroncache.integration.streaming.BidiWsStreamingHelper;

/**
 * Runs the shared put-item streaming suite against the bidirectional websocket endpoint: mutations still
 * go over HTTP, but streaming updates are observed via a {@code subscribe} frame on {@code /api/ws/v1/bidi}.
 * Demonstrates that the existing abstract streaming suites cover the BIDI subscribe/stream path unchanged.
 */
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = false, useClusteredMode = true, useTestContainersEnvironment = true)
class ClusteredBidiPutItemTests extends AbstractMultiStreamPutItemTests<String> {

    public ClusteredBidiPutItemTests() {
        super(new CacheTestEndpoints(), new BidiWsStreamingHelper(false));
    }

    @Override
    protected String getKnownCacheId() {
        return "ClusteredBidiPutItemTests-WS";
    }

    @Override
    protected String getKnownValue() {
        return "ClusteredBidiPutItemTests";
    }

    @Override
    protected String getAnotherKnownValue() {
        return "ClusteredBidiPutItemTests-second-value";
    }
}
