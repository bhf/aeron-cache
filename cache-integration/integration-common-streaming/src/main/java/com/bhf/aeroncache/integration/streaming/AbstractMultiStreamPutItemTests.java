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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamPutItemTests {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    private final StreamingHelper[] streamingHelpers;

    protected AbstractMultiStreamPutItemTests(StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
    }

    @BeforeAll
    static void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
    }

    @Test
    @DisplayName("Should get a streaming update when putting into a known cache")
    @HappyPath
    void shouldGetStreamingUpdateWhenPuttingIntoKnownCache(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = Arrays.stream(streamingHelpers)
                .map(helper -> helper.getEvents(backend, 1))
                .collect(Collectors.toList());

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);

        // Assert
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
                        var streamingSoureEvents = streamingSourceEventsFuture.get(60, TimeUnit.SECONDS);
                        assertOnSingleStreamingSourceEvents(streamingSoureEvents);
                    }
                });
    }

    private static void assertOnSingleStreamingSourceEvents(List<CacheUpdateEvent> streamingSoureEvents) {
        var updateEvent = streamingSoureEvents.get(0);
        MatcherAssert.assertThat("Expected event data to be available", updateEvent, Matchers.notNullValue());
        MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
        MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
        MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
        MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));
    }

}
