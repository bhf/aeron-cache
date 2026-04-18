package com.bhf.aeroncache.integration.ws;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.streaming.SSEStreamingHelper;
import com.bhf.aeroncache.integration.streaming.StreamingHelper;
import com.bhf.aeroncache.integration.streaming.StreamingHelperUtil;
import com.bhf.aeroncache.integration.streaming.WSStreamingHelper;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import com.bhf.aeroncache.integration.utils.ContainerRestartUtils;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

@ExtendWith(BackendTestLauncher.class)
abstract class MultiStreamRestartTests {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";
    private static final String ANOTHER_KNOWN_VALUE = "SomeOtherValue";

    private final SSEStreamingHelper sseStreamingHelper;
    private final WSStreamingHelper wsStreamingHelper;

    protected MultiStreamRestartTests(SSEStreamingHelper sseStreamingHelper, WSStreamingHelper wsStreamingHelper) {
        this.sseStreamingHelper = sseStreamingHelper;
        this.wsStreamingHelper = wsStreamingHelper;
    }

    @BeforeAll
    static void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
    }

    @Test
    @DisplayName("Should get a streaming update on SSE when WS is shutdown")
    @HappyPath
    void shouldGetStreamingUpdateOnSSEWhenWebsocketIsShutdown(BackendTestResource backend) {
        shouldGetUpdateOnSSEAfterWebsocketShutdown(backend);

        // Restart the websocket interface
        ContainerRestartUtils.awaitWSInterfaceRestart(backend);

        shouldGetUpdatesOnBothStreams(backend);
    }

    @Test
    @DisplayName("Should get a streaming update on WS when SSE is shutdown")
    @HappyPath
    void shouldGetStreamingUpdateOnWSWhenSSEIsShutdown(BackendTestResource backend) {
        shouldGetUpdateOnWSAfterSSEShutdown(backend);

        // Restart the sse interface
        ContainerRestartUtils.awaitSSEInterfaceRestart(backend);

        shouldGetUpdatesOnBothStreams(backend);
    }

    @HappyPath
    private void shouldGetUpdateOnSSEAfterWebsocketShutdown(BackendTestResource backend) {
        // Arrange
        StreamingHelper[] sseHelper = new StreamingHelper[]{sseStreamingHelper};
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(sseHelper, backend, 1);

        backend.getContainers().wsContainer().stop();

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY+"-sse", KNOWN_VALUE, backend);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat("Expected event data to be available", updateEvent, Matchers.notNullValue());
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY+"-sse"));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));
        }
    }

    @HappyPath
    private void shouldGetUpdateOnWSAfterSSEShutdown(BackendTestResource backend) {
        // Arrange
        StreamingHelper[] wsHelper = new StreamingHelper[]{wsStreamingHelper};
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(wsHelper, backend, 1);

        backend.getContainers().sseContainer().stop();

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY+"-ws", KNOWN_VALUE, backend);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat("Expected event data to be available", updateEvent, Matchers.notNullValue());
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY+"-ws"));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));
        }
    }

    @HappyPath
    private void shouldGetUpdatesOnBothStreams(BackendTestResource backend) {
        // Arrange
        StreamingHelper[] sseHelper = new StreamingHelper[]{sseStreamingHelper, wsStreamingHelper};
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(sseHelper, backend, 1);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, ANOTHER_KNOWN_VALUE, backend);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat("Expected event data to be available", updateEvent, Matchers.notNullValue());
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(ANOTHER_KNOWN_VALUE));
        }
    }

}
