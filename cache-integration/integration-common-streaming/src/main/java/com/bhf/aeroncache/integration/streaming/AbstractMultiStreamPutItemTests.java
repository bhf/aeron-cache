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
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY+"orderedtimer", KNOWN_VALUE, 5000, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, ANOTHER_KNOWN_KEY+"orderedtimer", ANOTHER_KNOWN_VALUE, 6000, backend);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY+"orderedtimer"));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));

            var secondUpdateEvent = streamingSoureEvents.get(1);
            MatcherAssert.assertThat(secondUpdateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondUpdateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(secondUpdateEvent.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY+"orderedtimer"));
            MatcherAssert.assertThat(secondUpdateEvent.itemValue(), Matchers.is(ANOTHER_KNOWN_VALUE));

            var firstRemove = streamingSoureEvents.get(2);
            MatcherAssert.assertThat(firstRemove.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(firstRemove.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(firstRemove.itemKey(), Matchers.is(KNOWN_KEY+"orderedtimer"));

            var secondRemove = streamingSoureEvents.get(3);
            MatcherAssert.assertThat(secondRemove.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(secondRemove.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(secondRemove.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY+"orderedtimer"));
        }
    }

    @Test
    @DisplayName("Should cancel old timer and use new one when putting same key with new TTL")
    @HappyPath
    protected void shouldCancelOldTimerWhenUpdatingTtl(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, KNOWN_CACHE_ID, 3);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY+"canceltimer", KNOWN_VALUE, 60000, backend);
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY+"canceltimer", ANOTHER_KNOWN_VALUE, 5000, backend);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();

            var firstAdd = streamingSourceEvents.get(0);
            MatcherAssert.assertThat(firstAdd.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(firstAdd.itemKey(), Matchers.is(KNOWN_KEY+"canceltimer"));
            MatcherAssert.assertThat(firstAdd.itemValue(), Matchers.is(KNOWN_VALUE));

            var secondAdd = streamingSourceEvents.get(1);
            MatcherAssert.assertThat(secondAdd.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondAdd.itemKey(), Matchers.is(KNOWN_KEY+"canceltimer"));
            MatcherAssert.assertThat(secondAdd.itemValue(), Matchers.is(ANOTHER_KNOWN_VALUE));

            var removeEvent = streamingSourceEvents.get(2);
            MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(removeEvent.itemKey(), Matchers.is(KNOWN_KEY+"canceltimer"));
        }
    }

}
