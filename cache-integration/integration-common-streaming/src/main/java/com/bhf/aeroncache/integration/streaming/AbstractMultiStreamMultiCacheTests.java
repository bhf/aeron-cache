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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamMultiCacheTests {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    private static final String ANOTHER_KNOWN_CACHE_ID = "AnotherCache";
    private static final String ANOTHER_KNOWN_KEY = "SomeOtherKey";
    private static final String ANOTHER_KNOWN_VALUE = "SomeOtherValue";

    private final StreamingHelper[] streamingHelpers;
    private final StreamingTestEndpointsProvider streamingEndpointsProvider;
    private TestEndpointsProvider multiStreamMultiCacheEndpoints;

    protected AbstractMultiStreamMultiCacheTests(TestEndpointsProvider endpointsProvider, StreamingTestEndpointsProvider streamingTestEndpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        this.streamingEndpointsProvider = streamingTestEndpointsProvider;
        multiStreamMultiCacheEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, multiStreamMultiCacheEndpoints);
        CacheTestUtils.createCache(ANOTHER_KNOWN_CACHE_ID, backend, multiStreamMultiCacheEndpoints);
    }

    @Test
    @DisplayName("Should get streaming updates on multi cache subscriptions")
    @HappyPath
    void shouldGetStreamingUpdatesOnMultiCacheSubscriptions(BackendTestResource backend) {
        // Arrange
        var cacheIds = List.of(KNOWN_CACHE_ID, ANOTHER_KNOWN_CACHE_ID);
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEventsMultipleCaches(streamingHelpers, backend, streamingEndpointsProvider, cacheIds, 2);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend, multiStreamMultiCacheEndpoints);
        CacheTestUtils.addItem(ANOTHER_KNOWN_CACHE_ID, ANOTHER_KNOWN_KEY, ANOTHER_KNOWN_VALUE, backend, multiStreamMultiCacheEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();

            MatcherAssert.assertThat(streamingSourceEvents.size(), Matchers.is(2));

            // Assert on the first event (from KNOWN_CACHE_ID)
            var event1 = streamingSourceEvents.stream()
                    .filter(e -> e.cacheId().equals(KNOWN_CACHE_ID))
                    .findFirst()
                    .orElseThrow();
            MatcherAssert.assertThat(event1.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event1.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(event1.itemValue(), Matchers.is(KNOWN_VALUE));

            // Assert on the second event (from ANOTHER_KNOWN_CACHE_ID)
            var event2 = streamingSourceEvents.stream()
                    .filter(e -> e.cacheId().equals(ANOTHER_KNOWN_CACHE_ID))
                    .findFirst()
                    .orElseThrow();
            MatcherAssert.assertThat(event2.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event2.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY));
            MatcherAssert.assertThat(event2.itemValue(), Matchers.is(ANOTHER_KNOWN_VALUE));
        }
    }


}
