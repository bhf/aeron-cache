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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractPutItemTests {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    private final StreamingHelper streamingHelper;
    private TestEndpointsProvider putItemsEndpoints;

    protected AbstractPutItemTests(TestEndpointsProvider endpointsProvider, StreamingHelper streamingHelper) {
        this.streamingHelper = streamingHelper;
        putItemsEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, putItemsEndpoints);
    }

    @Test
    @DisplayName("Should get a streaming update when putting into a known cache")
    @HappyPath
    void shouldGetStreamingUpdateWhenPuttingIntoKnownCache(BackendTestResource backend) {
        // Arrange
        var readyFuture = new CompletableFuture<Void>();
        var eventData = streamingHelper.getEvents(backend, KNOWN_CACHE_ID, 1, readyFuture);

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(readyFuture::isDone);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend, putItemsEndpoints);

        // Assert
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                            var updateEvents = eventData.get();
                            var updateEvent = updateEvents.get(0);
                            MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
                            MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
                            MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY));
                            MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE));
                        }
                );
    }

}
