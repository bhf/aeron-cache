package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamPutItemTests {

    public static final String HYDRATION_CACHE_ID = "hydration-cache";
    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    private static final String ANOTHER_KNOWN_KEY = "SomeOtherKey";
    private static final String ANOTHER_KNOWN_VALUE = "SomeOtherValue";

    private final StreamingHelper[] streamingHelpers;

    protected AbstractMultiStreamPutItemTests(StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
    }

    @BeforeAll
    static void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        CacheTestUtils.createCache(HYDRATION_CACHE_ID, backend);
    }

    @Test
    @DisplayName("Should get a streaming updates when putting into a known cache")
    @HappyPath
    void shouldGetStreamingUpdateWhenPuttingIntoKnownCache(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, KNOWN_CACHE_ID, 1);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);

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
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));
        }
    }

    @Test
    @DisplayName("Should get a streaming updates when putting on existing key into a known cache")
    @HappyPath
    void shouldGetStreamingUpdateWhenPuttingExistingKeyIntoKnownCache(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, KNOWN_CACHE_ID, 2);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, ANOTHER_KNOWN_VALUE, backend);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));

            var secondUpdateEvent = streamingSoureEvents.get(1);
            MatcherAssert.assertThat(secondUpdateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondUpdateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(secondUpdateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(secondUpdateEvent.itemValue(), Matchers.is(ANOTHER_KNOWN_VALUE));
        }
    }

    @Test
    @DisplayName("Should get ordered add and remove streaming updates on put items with a ttl")
    @HappyPath
    protected void shouldGetOrderedUpdatesOnTimedRemoved(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, KNOWN_CACHE_ID, 4);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, 5000, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, ANOTHER_KNOWN_KEY, ANOTHER_KNOWN_VALUE, 6000, backend);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));

            var secondUpdateEvent = streamingSoureEvents.get(1);
            MatcherAssert.assertThat(secondUpdateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondUpdateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(secondUpdateEvent.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY));
            MatcherAssert.assertThat(secondUpdateEvent.itemValue(), Matchers.is(ANOTHER_KNOWN_VALUE));

            var firstRemove = streamingSoureEvents.get(2);
            MatcherAssert.assertThat(firstRemove.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(firstRemove.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(firstRemove.itemKey(), Matchers.is(KNOWN_KEY));

            var secondRemove = streamingSoureEvents.get(3);
            MatcherAssert.assertThat(secondRemove.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(secondRemove.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(secondRemove.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY));
        }
    }

    @Test
    @DisplayName("Should get hydrated streaming updates when subscribing to a cache with existing items")
    @HappyPath
    void shouldGetHydratedStreamingUpdateWithExistingState(BackendTestResource backend) {
        // Arrange
        var hydrationKey1 = "HydrationKey1";
        var hydrationValue1 = "HydrationValue1";
        var hydrationKey2 = "HydrationKey2";
        var hydrationValue2 = "HydrationValue2";

        CacheTestUtils.addItem(HYDRATION_CACHE_ID, hydrationKey1, hydrationValue1, backend);
        CacheTestUtils.addItem(HYDRATION_CACHE_ID, hydrationKey2, hydrationValue2, backend);

        // Act
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEventsWithHydration(streamingHelpers, backend, HYDRATION_CACHE_ID, 2);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            MatcherAssert.assertThat(streamingSoureEvents, Matchers.hasSize(2));

            for (var event : streamingSoureEvents) {
                MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
                MatcherAssert.assertThat(event.cacheId(), Matchers.is(HYDRATION_CACHE_ID));
            }

            var eventMap = streamingSoureEvents.stream().collect(
                    Collectors.toMap(CacheUpdateEvent::itemKey, CacheUpdateEvent::itemValue));

            MatcherAssert.assertThat(eventMap.get(hydrationKey1), Matchers.is(hydrationValue1));
            MatcherAssert.assertThat(eventMap.get(hydrationKey2), Matchers.is(hydrationValue2));
        }
    }

}
