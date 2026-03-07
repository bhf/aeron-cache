package com.bhf.aeroncache.integration.sse;

import com.bhf.aeroncache.integration.clients.SSEStreamingHelper;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.streaming.AbstractPutItemTests;

@BackendTestConfig(httpEnabled = true, wsEnabled = false, sseEnabled = true)
class PutItemTests extends AbstractPutItemTests {

    public PutItemTests() {
        super(new SSEStreamingHelper());
    }
}
