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
public abstract class AbstractMultiStreamMultiCacheTests<V> {
    
    private static final String KNOWN_KEY = "SomeKey";
    
    private static final String ANOTHER_KNOWN_KEY = "SomeOtherKey";

    private final StreamingHelper[] streamingHelpers;
    private TestEndpointsProvider multiStreamMultiCacheEndpoints;

    protected AbstractMultiStreamMultiCacheTests(TestEndpointsProvider endpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        multiStreamMultiCacheEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(getKnownCacheId(), backend, multiStreamMultiCacheEndpoints);
        CacheTestUtils.createCache(getAnotherKnownCacheId(), backend, multiStreamMultiCacheEndpoints);
    }

    protected abstract String getKnownCacheId();

    protected abstract String getAnotherKnownCacheId();

    protected abstract V getAnotherKnownValue();

    protected abstract V getKnownValue();

    @Test
    @DisplayName("Should get streaming updates on multi cache subscriptions")
    @HappyPath
    void shouldGetStreamingUpdatesOnMultiCacheSubscriptions(BackendTestResource backend) {
        // Arrange
        var cacheIds = List.of(getKnownCacheId(), getAnotherKnownCacheId());
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEventsMultipleCaches(streamingHelpers, backend, cacheIds, 2);

        // Act
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY, getKnownValue(), backend, multiStreamMultiCacheEndpoints);
        CacheTestUtils.addItem(getAnotherKnownCacheId(), ANOTHER_KNOWN_KEY, getAnotherKnownValue(), backend, multiStreamMultiCacheEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();

            MatcherAssert.assertThat(streamingSourceEvents.size(), Matchers.is(2));

            // Assert on the first event (from getKnownCacheId())
            var event1 = streamingSourceEvents.stream()
                    .filter(e -> e.cacheId().equals(getKnownCacheId()))
                    .findFirst()
                    .orElseThrow();
            MatcherAssert.assertThat(event1.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event1.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(event1.itemValue(), Matchers.is(getKnownValue()));

            // Assert on the second event (from getAnotherKnownCacheId())
            var event2 = streamingSourceEvents.stream()
                    .filter(e -> e.cacheId().equals(getAnotherKnownCacheId()))
                    .findFirst()
                    .orElseThrow();
            MatcherAssert.assertThat(event2.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event2.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY));
            MatcherAssert.assertThat(event2.itemValue(), Matchers.is(getAnotherKnownValue()));
        }
    }

}
