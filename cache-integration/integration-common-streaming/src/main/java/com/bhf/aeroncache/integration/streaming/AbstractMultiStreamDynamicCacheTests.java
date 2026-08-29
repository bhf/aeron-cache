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
public abstract class AbstractMultiStreamDynamicCacheTests<V> {

    private static final String DYNAMIC_CACHE_ID = "1-dynamic";
    private static final String KNOWN_KEY = "SomeKey";


    private final StreamingHelper[] streamingHelpers;
    private TestEndpointsProvider multiStreamDynamicCacheEndpoints;

    protected AbstractMultiStreamDynamicCacheTests(TestEndpointsProvider endpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        multiStreamDynamicCacheEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.deleteCache(DYNAMIC_CACHE_ID, backend, multiStreamDynamicCacheEndpoints);
    }

    @Test
    @DisplayName("Should get a streaming updates when putting into a dynamically created cache")
    @HappyPath
    void shouldGetStreamingUpdateWhenPuttingIntoDynamicallyCreatedCache(BackendTestResource backend) {
        // Arrange
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, DYNAMIC_CACHE_ID, 1);

        // Act
        CacheTestUtils.addItem(DYNAMIC_CACHE_ID, KNOWN_KEY, getKnownValue(), backend, multiStreamDynamicCacheEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSoureEvents = streamingSourceEventsFuture.join();

            var updateEvent = streamingSoureEvents.get(0);
            MatcherAssert.assertThat("Expected event data to be available", updateEvent, Matchers.notNullValue());
            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(DYNAMIC_CACHE_ID));
            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(getKnownValue()));
        }
    }

    protected abstract V getKnownValue();

}
