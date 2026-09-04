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

import java.util.List;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamRemoveItemTest<V> {

    private static final String KNOWN_KEY = "SomeKey";

    private final StreamingHelper[] streamingHelpers;
    private TestEndpointsProvider removeEndpoints;

    protected AbstractMultiStreamRemoveItemTest(TestEndpointsProvider endpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        removeEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(getKnownCacheId(), backend, removeEndpoints);
    }

    protected abstract String getKnownCacheId();

    @Test
    @DisplayName("Should get a streaming update when removing an existing item")
    @HappyPath
    void shouldGetStreamingUpdateWhenRemovingExistingItem(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 2);

        // Act
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY, getKnownValue(), backend, removeEndpoints);
        CacheTestUtils.removeItem(getKnownCacheId(), KNOWN_KEY, backend, removeEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            assertOnSingleStreamingSourceEvents(streamingSourceEventsFuture.join(), getKnownCacheId(), getKnownValue());
        }
    }


    private static <V> void assertOnSingleStreamingSourceEvents(List<CacheUpdateEvent> streamingSoureEvents, String knownCacheId, V knownValue) {
        var addEvent = streamingSoureEvents.get(0);
        MatcherAssert.assertThat(addEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
        MatcherAssert.assertThat(addEvent.cacheId(), Matchers.is(knownCacheId));
        MatcherAssert.assertThat(addEvent.itemKey(), Matchers.is(KNOWN_KEY));
        MatcherAssert.assertThat(addEvent.itemValue(), Matchers.is(knownValue));

        var removeEvent = streamingSoureEvents.get(1);
        MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
        MatcherAssert.assertThat(removeEvent.cacheId(), Matchers.is(knownCacheId));
        MatcherAssert.assertThat(removeEvent.itemKey(), Matchers.is(KNOWN_KEY));
    }

    public abstract V getKnownValue();
}
