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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamHydrationTests {

    private final StreamingHelper[] streamingHelpers;
    private static final String HYDRATION_CACHE = "hydration-cache";
    private final StreamingTestEndpointsProvider streamingEndpointsProvider;
    private TestEndpointsProvider hydratingStreamEndpoints;

    protected AbstractMultiStreamHydrationTests(TestEndpointsProvider endpointsProvider, StreamingTestEndpointsProvider streamingTestEndpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        this.streamingEndpointsProvider = streamingTestEndpointsProvider;
        hydratingStreamEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(HYDRATION_CACHE, backend, hydratingStreamEndpoints);
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

        CacheTestUtils.addItem(HYDRATION_CACHE, hydrationKey1, hydrationValue1, backend, hydratingStreamEndpoints);
        CacheTestUtils.addItem(HYDRATION_CACHE, hydrationKey2, hydrationValue2, backend, hydratingStreamEndpoints);

        // Act
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEventsWithHydration(streamingHelpers, backend, streamingEndpointsProvider, HYDRATION_CACHE, 2);

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

    @Test
    @DisplayName("Should get hydrated streaming updates when subscribing to multiple caches with existing items")
    @HappyPath
    void shouldGetHydratedStreamingUpdateWithMultipleCaches(BackendTestResource backend) {
        // Arrange
        var cache1 = "multi-hydration-cache-1";
        var cache2 = "multi-hydration-cache-2";
        CacheTestUtils.createCache(cache1, backend, hydratingStreamEndpoints);
        CacheTestUtils.createCache(cache2, backend, hydratingStreamEndpoints);

        var cache1Key = "cache1Key";
        var cache1Value = "cache1Value";
        var cache2Key = "cache2Key";
        var cache2Value = "cache2Value";

        CacheTestUtils.addItem(cache1, cache1Key, cache1Value, backend, hydratingStreamEndpoints);
        CacheTestUtils.addItem(cache2, cache2Key, cache2Value, backend, hydratingStreamEndpoints);

        // Act
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEventsMultipleCachesWithHydration(
                streamingHelpers, backend, streamingEndpointsProvider, List.of(cache1, cache2), 2);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();

            MatcherAssert.assertThat(streamingSourceEvents, Matchers.hasSize(2));

            var eventMap = streamingSourceEvents.stream().collect(
                    Collectors.toMap(e -> e.cacheId() + ":" + e.itemKey(), CacheUpdateEvent::itemValue));

            MatcherAssert.assertThat(eventMap.get(cache1 + ":" + cache1Key), Matchers.is(cache1Value));
            MatcherAssert.assertThat(eventMap.get(cache2 + ":" + cache2Key), Matchers.is(cache2Value));
        }
    }
}
