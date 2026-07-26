package com.bhf.aeroncache.integration.shutdown;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.BackendTestConfig;
import com.bhf.aeroncache.integration.config.CacheTestEndpoints;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
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
 * Integration test to verify that the SSE interface can handle the cluster
 * being restarted without itself being restarted.
 */
@ExtendWith(BackendTestLauncher.class)
@BackendTestConfig(httpEnabled = true, wsEnabled = true, sseEnabled = true, useClusteredMode = true, useTestContainersEnvironment = true)
class SseClientClusterRestartTests {

    static final String KNOWN_CACHE_ID = "SSERestartCache★";
    static final String KNOWN_KEY = "SSERestartKey★★★";
    static final String KNOWN_VALUE = "SSERestartValue★★★";
    private TestEndpointsProvider sseRestartEndpointsProvider = new CacheTestEndpoints();

    @Test
    @DisplayName("Should get streaming updates via SSE after cluster restart")
    @HappyPath
    void shouldHandleClusterRestart(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, sseRestartEndpointsProvider);

        // Act
        ContainerRestartUtils.stopClusterContainers(backend);
        ContainerRestartUtils.awaitAeronCacheClusterRestart(backend);

        // Use readiness endpoint to wait for the SSE interface to detect the reconnection to the cluster
        ContainerRestartUtils.awaitSSEReadiness(backend);

        // Generate an event
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend, sseRestartEndpointsProvider);

        var streamingHelpers = new StreamingHelper[]{new SSEStreamingHelper()};
        var postRestartEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, KNOWN_CACHE_ID, 1);

        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend, sseRestartEndpointsProvider);

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
