package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.StreamingTestEndpointsProvider;
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
public abstract class AbstractMultiStreamPutItemTests<V> {

    private static final String KNOWN_KEY = "SomeKey";
    private static final String ANOTHER_KNOWN_KEY = "SomeOtherKey";

    private final TestEndpointsProvider putItemsEndpoints;
    private final StreamingHelper[] streamingHelpers;

    protected AbstractMultiStreamPutItemTests(TestEndpointsProvider endpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        putItemsEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(getKnownCacheId(), backend, putItemsEndpoints);
    }

    protected abstract String getKnownCacheId();

    @Test
    @DisplayName("Should get a streaming updates when putting into a known cache")
    @HappyPath
    protected void shouldGetStreamingUpdateWhenPuttingIntoKnownCache(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 1);

        // Act
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY, getKnownValue(), backend, putItemsEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat("Expected event data to be available", updateEvent, Matchers.notNullValue());
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(getKnownValue()));
        }
    }

    @Test
    @DisplayName("Should get a streaming updates when putting on existing key into a known cache")
    @HappyPath
    protected void shouldGetStreamingUpdateWhenPuttingExistingKeyIntoKnownCache(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 2);

        // Act
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY, getKnownValue(), backend, putItemsEndpoints);
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY, getAnotherKnownValue(), backend, putItemsEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(getKnownValue()));

            var secondUpdateEvent = streamingSoureEvents.get(1);
            MatcherAssert.assertThat(secondUpdateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondUpdateEvent.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(secondUpdateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(secondUpdateEvent.itemValue(), Matchers.is(getAnotherKnownValue()));
        }
    }

    @Test
    @DisplayName("Should get ordered add and remove streaming updates on put items with a ttl")
    @HappyPath
    protected void shouldGetOrderedUpdatesOnTimedRemoved(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 4);

        // Act
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY+"orderedtimer", getKnownValue(), 5000, backend, putItemsEndpoints);
        CacheTestUtils.addItem(getKnownCacheId(), ANOTHER_KNOWN_KEY+"orderedtimer", getAnotherKnownValue(), 6000, backend, putItemsEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY+"orderedtimer"));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(getKnownValue()));

            var secondUpdateEvent = streamingSoureEvents.get(1);
            MatcherAssert.assertThat(secondUpdateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondUpdateEvent.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(secondUpdateEvent.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY+"orderedtimer"));
            MatcherAssert.assertThat(secondUpdateEvent.itemValue(), Matchers.is(getAnotherKnownValue()));

            var firstRemove = streamingSoureEvents.get(2);
            MatcherAssert.assertThat(firstRemove.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(firstRemove.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(firstRemove.itemKey(), Matchers.is(KNOWN_KEY+"orderedtimer"));

            var secondRemove = streamingSoureEvents.get(3);
            MatcherAssert.assertThat(secondRemove.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(secondRemove.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(secondRemove.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY+"orderedtimer"));
        }
    }

    @Test
    @DisplayName("Should cancel old timer and use new one when putting same key with new TTL")
    @HappyPath
    protected void shouldCancelOldTimerWhenUpdatingTtl(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 3);

        // Act
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY+"canceltimer", getKnownValue(), 60000, backend, putItemsEndpoints);
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY+"canceltimer", getAnotherKnownValue(), 5000, backend, putItemsEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();

            var firstAdd = streamingSourceEvents.get(0);
            MatcherAssert.assertThat(firstAdd.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(firstAdd.itemKey(), Matchers.is(KNOWN_KEY+"canceltimer"));
            MatcherAssert.assertThat(firstAdd.itemValue(), Matchers.is(getKnownValue()));

            var secondAdd = streamingSourceEvents.get(1);
            MatcherAssert.assertThat(secondAdd.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondAdd.itemKey(), Matchers.is(KNOWN_KEY+"canceltimer"));
            MatcherAssert.assertThat(secondAdd.itemValue(), Matchers.is(getAnotherKnownValue()));

            var removeEvent = streamingSourceEvents.get(2);
            MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(removeEvent.itemKey(), Matchers.is(KNOWN_KEY+"canceltimer"));
        }
    }


    @Test
    @DisplayName("Should get a streaming update carrying the merged value when patching an item")
    @HappyPath
    protected void shouldGetStreamingUpdateWhenPatchingItem(BackendTestResource backend) {
        // Arrange
        var patchKey = KNOWN_KEY + "patch";
        var initialValue = "{\"a\":1,\"b\":2}";
        var patch = "{\"b\":3,\"c\":4}";
        var mergedValue = "{\"a\":1,\"b\":3,\"c\":4}";
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 2);

        // Act
        CacheTestUtils.addItem(getKnownCacheId(), patchKey, initialValue, backend, putItemsEndpoints);
        CacheTestUtils.patchItem(getKnownCacheId(), patchKey, patch, backend, putItemsEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            // The initial add is streamed as an ADD_ITEM with the original value.
            var addEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat(addEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(addEvent.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(addEvent.itemKey(), Matchers.is(patchKey));
            MatcherAssert.assertThat(addEvent.itemValue(), Matchers.is(initialValue));

            // The patch is streamed as an ADD_ITEM carrying the merged value.
            var patchEvent = streamingSoureEvents.get(1);
            MatcherAssert.assertThat(patchEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(patchEvent.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(patchEvent.itemKey(), Matchers.is(patchKey));
            MatcherAssert.assertThat(patchEvent.itemValue(), Matchers.is(mergedValue));
        }
    }

    protected abstract V getKnownValue();
    protected abstract V getAnotherKnownValue();

}
