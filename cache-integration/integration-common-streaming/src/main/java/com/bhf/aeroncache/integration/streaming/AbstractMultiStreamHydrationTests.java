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
public abstract class AbstractMultiStreamHydrationTests {

    private final StreamingHelper[] streamingHelpers;
    private static final String HYDRATION_CACHE = "hydration-cache";

    protected AbstractMultiStreamHydrationTests(StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
    }

    @BeforeAll
    static void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(HYDRATION_CACHE, backend);
    }

    @Test
    @DisplayName("Should get hydrated streaming updates when subscribing to a cache with existing items")
    @HappyPath
    void shouldGetHydratedStreamingUpdateWithExistingState(BackendTestResource backend) {
        // Arrange

        CacheTestUtils.createCache(HYDRATION_CACHE, backend);

        var hydrationKey1 = "HydrationKey1";
        var hydrationValue1 = "HydrationValue1";
        var hydrationKey2 = "HydrationKey2";
        var hydrationValue2 = "HydrationValue2";

        CacheTestUtils.addItem(HYDRATION_CACHE, hydrationKey1, hydrationValue1, backend);
        CacheTestUtils.addItem(HYDRATION_CACHE, hydrationKey2, hydrationValue2, backend);

        // Act
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEventsWithHydration(streamingHelpers, backend, HYDRATION_CACHE, 2);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            MatcherAssert.assertThat(streamingSoureEvents, Matchers.hasSize(2));

            for (var event : streamingSoureEvents) {
                MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
                MatcherAssert.assertThat(event.cacheId(), Matchers.is(HYDRATION_CACHE));
            }

            var eventMap = streamingSoureEvents.stream().collect(
                    Collectors.toMap(CacheUpdateEvent::itemKey, CacheUpdateEvent::itemValue));

            MatcherAssert.assertThat(eventMap.get(hydrationKey1), Matchers.is(hydrationValue1));
            MatcherAssert.assertThat(eventMap.get(hydrationKey2), Matchers.is(hydrationValue2));
        }
    }
}
