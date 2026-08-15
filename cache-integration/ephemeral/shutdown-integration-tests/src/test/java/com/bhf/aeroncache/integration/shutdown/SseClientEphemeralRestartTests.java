package com.bhf.aeroncache.integration.shutdown;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.*;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.StreamingHelper;
import com.bhf.aeroncache.integration.streaming.StreamingHelperUtil;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import com.bhf.aeroncache.integration.utils.ContainerRestartUtils;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

/**
 * Integration test to verify that the SSE interface can handle the ephemeral cache
 * being restarted without itself being restarted.
 */
@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = false, useTestContainersEnvironment = true)
class SseClientEphemeralRestartTests {

    static final String KNOWN_CACHE_ID = "SSERestartCache★";
    static final String KNOWN_KEY = "SSERestartKey★★★";
    static final String KNOWN_VALUE = "SSERestartValue★★★";
    private final TestEndpointsProvider endpointsProvider = new CacheTestEndpoints();
    private final StreamingTestEndpointsProvider streamingTestEndpointsProvider = new StreamingCacheTestEndpoints();

    @Test
    @DisplayName("Should get streaming updates via SSE after ephemeral cache restart")
    @HappyPath
    void shouldHandleClusterRestart(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, endpointsProvider);

        // Act
        ContainerRestartUtils.stopEphemeralContainers(backend);
        ContainerRestartUtils.awaitEphemeralAeronCacheClusterRestart(backend);

        // Use readiness endpoint to wait for the SSE interface to detect the reconnection to the cluster
        ContainerRestartUtils.awaitSSEReadiness(backend);

        var streamingHelpers = new StreamingHelper[]{new SSEStreamingHelper()};
        var postRestartEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, streamingTestEndpointsProvider, KNOWN_CACHE_ID, 1);

        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend, endpointsProvider);

        // Assert
        for (var streamingSourceEventsFuture : postRestartEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));
        }
    }
}
