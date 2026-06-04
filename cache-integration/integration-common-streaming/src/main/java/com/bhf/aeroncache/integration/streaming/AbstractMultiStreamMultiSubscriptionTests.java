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

import java.util.List;
import java.util.concurrent.TimeUnit;

@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamMultiSubscriptionTests {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    private static final String ANOTHER_KNOWN_CACHE_ID = "AnotherCache";
    private static final String ANOTHER_KNOWN_KEY = "SomeOtherKey";
    private static final String ANOTHER_KNOWN_VALUE = "SomeOtherValue";

    private final StreamingHelper[] streamingHelpers;

    protected AbstractMultiStreamMultiSubscriptionTests(StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
    }

    @BeforeAll
    static void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend);
        CacheTestUtils.createCache(ANOTHER_KNOWN_CACHE_ID, backend);
    }

    @Test
    @DisplayName("Should get streaming updates from multiple individual subscriptions")
    @HappyPath
    void shouldGetStreamingUpdatesFromMultipleIndividualSubscriptions(BackendTestResource backend) {
        // Arrange
        var subscription1 = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, KNOWN_CACHE_ID, 1);
        var subscription2 = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, ANOTHER_KNOWN_CACHE_ID, 1);

        // Act
        CacheTestUtils.addItem(KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE, backend);
        CacheTestUtils.addItem(ANOTHER_KNOWN_CACHE_ID, ANOTHER_KNOWN_KEY, ANOTHER_KNOWN_VALUE, backend);

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
            MatcherAssert.assertThat(event1.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(event1.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event1.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(event1.itemValue(), Matchers.is(KNOWN_VALUE));

            MatcherAssert.assertThat(events2.size(), Matchers.is(1));
            var event2 = events2.get(0);
            MatcherAssert.assertThat(event2.cacheId(), Matchers.is(ANOTHER_KNOWN_CACHE_ID));
            MatcherAssert.assertThat(event2.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event2.itemKey(), Matchers.is(ANOTHER_KNOWN_KEY));
            MatcherAssert.assertThat(event2.itemValue(), Matchers.is(ANOTHER_KNOWN_VALUE));
        }
    }
}
