package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamIncrementCounterTests {

    private static final String KNOWN_KEY = "SomeCounter";
    private static final String ACCUMULATING_KEY = "AccumulatingCounter";

    private final TestEndpointsProvider countersEndpoints;
    private final StreamingHelper[] streamingHelpers;

    protected AbstractMultiStreamIncrementCounterTests(TestEndpointsProvider endpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        this.countersEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(getKnownCacheId(), backend, countersEndpoints);
    }

    protected abstract String getKnownCacheId();

    @Test
    @DisplayName("Should get a streaming update when incrementing a counter in a known cache")
    @HappyPath
    protected void shouldGetStreamingUpdateWhenIncrementingCounter(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY, 0, backend, countersEndpoints);
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 1);

        // Act
        CacheTestUtils.incrementCounter(getKnownCacheId(), KNOWN_KEY, 5, backend, countersEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSourceEvents.get(0);
            MatcherAssert.assertThat("Expected event data to be available", updateEvent, Matchers.notNullValue());
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(5));
        }
    }

    @Test
    @DisplayName("Should get ordered streaming updates when accumulating multiple increments")
    @HappyPath
    protected void shouldGetStreamingUpdatesWhenAccumulatingIncrements(BackendTestResource backend) {
        // Arrange
        CacheTestUtils.addItem(getKnownCacheId(), ACCUMULATING_KEY, 0, backend, countersEndpoints);
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 2);

        // Act
        CacheTestUtils.incrementCounter(getKnownCacheId(), ACCUMULATING_KEY, 5, backend, countersEndpoints);
        CacheTestUtils.incrementCounter(getKnownCacheId(), ACCUMULATING_KEY, 3, backend, countersEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();

            var firstUpdate = streamingSourceEvents.get(0);
            MatcherAssert.assertThat(firstUpdate.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(firstUpdate.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(firstUpdate.itemKey(), Matchers.is(ACCUMULATING_KEY));
            MatcherAssert.assertThat(firstUpdate.itemValue(), Matchers.is(5));

            var secondUpdate = streamingSourceEvents.get(1);
            MatcherAssert.assertThat(secondUpdate.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondUpdate.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(secondUpdate.itemKey(), Matchers.is(ACCUMULATING_KEY));
            MatcherAssert.assertThat(secondUpdate.itemValue(), Matchers.is(8));
        }
    }

}
