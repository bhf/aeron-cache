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
public abstract class AbstractMultiStreamDeleteCacheTest<V> {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";

    private final StreamingHelper[] streamingHelpers;
    private final StreamingTestEndpointsProvider streamingEndpointsProvider;
    private TestEndpointsProvider streamDeleteEndpoints;

    protected AbstractMultiStreamDeleteCacheTest(TestEndpointsProvider endpointsProvider, StreamingTestEndpointsProvider streamingTestEndpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        this.streamingEndpointsProvider = streamingTestEndpointsProvider;
        streamDeleteEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, streamDeleteEndpoints);
    }

    @Test
    @DisplayName("Should get a streaming update when deleting an existing cache")
    @HappyPath
    void shouldGetStreamingUpdateWhenDeletingExistingCache(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, streamingEndpointsProvider, KNOWN_CACHE_ID, 2);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, getKnownValue(), backend, streamDeleteEndpoints);
        CacheTestUtils.deleteCache(KNOWN_CACHE_ID, backend, streamDeleteEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            assertOnSingleStreamingSourceEvents(streamingSourceEventsFuture.join(), getKnownValue());
        }
    }


    private static <V> void assertOnSingleStreamingSourceEvents(List<CacheUpdateEvent> streamingSoureEvents, V knownValue) {
        var addEvent = streamingSoureEvents.get(0);
        MatcherAssert.assertThat(addEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
        MatcherAssert.assertThat(addEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
        MatcherAssert.assertThat(addEvent.itemKey(), Matchers.is(KNOWN_KEY));
        MatcherAssert.assertThat(addEvent.itemValue(), Matchers.is(knownValue));

        var removeEvent = streamingSoureEvents.get(1);
        MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.DELETE_CACHE));
        MatcherAssert.assertThat(removeEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
    }

    public abstract V getKnownValue();
}
