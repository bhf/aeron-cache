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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamRemoveItemTest {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    private final StreamingHelper[] streamingHelpers;

    protected AbstractMultiStreamRemoveItemTest(StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
    }

    @BeforeAll
    static void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
    }

    @Test
    @DisplayName("Should get a streaming update when removing an existing item")
    @HappyPath
    void shouldGetStreamingUpdateWhenRemovingExistingItem(BackendTestResource backend) {
        // Arrange
        List<CompletableFuture<Void>> readyFutures = new ArrayList<>();
        var perStreamingSourceEvents = Arrays.stream(streamingHelpers)
                .map(helper -> {
                    CompletableFuture<Void> ready = new CompletableFuture<>();
                    readyFutures.add(ready);
                    return helper.getEvents(backend, 2, ready);
                })
                .collect(Collectors.toList());

        readyFutures.forEach(f -> Awaitility.await().atMost(60, TimeUnit.SECONDS).until(f::isDone));

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);
        CacheTestUtils.removeItem(KNOWN_CACHE_ID, KNOWN_KEY, backend);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            assertOnSingleStreamingSourceEvents(streamingSourceEventsFuture.join());
        }
    }


    private static void assertOnSingleStreamingSourceEvents(List<CacheUpdateEvent> streamingSoureEvents) {
        var addEvent = streamingSoureEvents.get(0);
        MatcherAssert.assertThat(addEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
        MatcherAssert.assertThat(addEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
        MatcherAssert.assertThat(addEvent.itemKey(), Matchers.is(KNOWN_KEY));
        MatcherAssert.assertThat(addEvent.itemValue(), Matchers.is(KNOWN_VALUE));

        var removeEvent = streamingSoureEvents.get(1);
        MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
        MatcherAssert.assertThat(removeEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
        MatcherAssert.assertThat(removeEvent.itemKey(), Matchers.is(KNOWN_KEY));
    }

}
