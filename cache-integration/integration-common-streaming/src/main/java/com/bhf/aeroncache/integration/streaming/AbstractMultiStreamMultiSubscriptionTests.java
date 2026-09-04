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

import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamMultiSubscriptionTests<V> {

    private static final String KNOWN_KEY = "SomeKey";
    private static final String ANOTHER_KNOWN_KEY = "SomeOtherKey";

    private final StreamingHelper[] streamingHelpers;
    private TestEndpointsProvider subsEndpoints;

    protected AbstractMultiStreamMultiSubscriptionTests(TestEndpointsProvider endpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        subsEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(getKnownCacheId(), backend, subsEndpoints);
        CacheTestUtils.createCache(getAnotherKnownCacheId(), backend, subsEndpoints);
    }

    protected abstract String getKnownCacheId();

    protected abstract String getAnotherKnownCacheId();

    protected abstract V getAnotherKnownValue();

    protected abstract V getKnownValue();

    @Test
    @DisplayName("Should get streaming updates from multiple individual subscriptions")
    @HappyPath
    void shouldGetStreamingUpdatesFromMultipleIndividualSubscriptions(BackendTestResource backend) {
        // Arrange
        var subscription1 = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getKnownCacheId(), 1);
        var subscription2 = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, getAnotherKnownCacheId(), 1);

        // Act
        CacheTestUtils.addItem(getKnownCacheId(), KNOWN_KEY, getKnownValue(), backend, subsEndpoints);
        CacheTestUtils.addItem(getAnotherKnownCacheId(), ANOTHER_KNOWN_KEY, getAnotherKnownValue(), backend, subsEndpoints);

        // Assert
        for (int i = 0; i < streamingHelpers.length; i++) {
            var events1Future = subscription1.get(i);
            var events2Future = subscription2.get(i);

            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(() -> events1Future.isDone() && events2Future.isDone());

            var events1 = events1Future.join();
            var events2 = events2Future.join();

            MatcherAssert.assertThat(events1.size(), Matchers.is(1));
            var event1 = events1.get(0);
            MatcherAssert.assertThat(event1.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(event1.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event1.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(event1.itemValue(), Matchers.is(getKnownValue()));

            MatcherAssert.assertThat(events2.size(), Matchers.is(1));
            var event2 = events2.get(0);
            MatcherAssert.assertThat(event2.cacheId(), Matchers.is(getAnotherKnownCacheId()));
            MatcherAssert.assertThat(event2.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event2.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY));
            MatcherAssert.assertThat(event2.itemValue(), Matchers.is(getAnotherKnownValue()));
        }
    }
}
