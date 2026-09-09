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

/**
 * Verifies key-scoped subscriptions: a client that subscribes to a specific key within a cache
 * (via the {@code ?keys=} query parameter) only receives updates for that key, not for other keys
 * in the same cache.
 *
 * @param <V> The cache value type.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamKeySubscriptionTests<V> {

    private static final String SUBSCRIBED_KEY = "subscribedKey";
    private static final String OTHER_KEY = "otherKey";

    private final StreamingHelper[] streamingHelpers;
    private final TestEndpointsProvider subsEndpoints;

    protected AbstractMultiStreamKeySubscriptionTests(TestEndpointsProvider endpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        this.subsEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(getKnownCacheId(), backend, subsEndpoints);
    }

    protected abstract String getKnownCacheId();

    protected abstract V getSubscribedValue();

    protected abstract V getOtherValue();

    @Test
    @DisplayName("Should only receive updates for the specific key subscribed to")
    @HappyPath
    void shouldOnlyReceiveUpdatesForSubscribedKey(BackendTestResource backend) {
        // Arrange: subscribe to a single key, expecting exactly one event.
        var subscription = StreamingHelperUtil.getPerStreamEventsForKeys(
                streamingHelpers, backend, getKnownCacheId(), List.of(SUBSCRIBED_KEY), 1);

        // Act: add the non-subscribed key first, then the subscribed key. The key filter must drop
        // the former, so the single delivered event is guaranteed to be the subscribed key.
        CacheTestUtils.addItem(getKnownCacheId(), OTHER_KEY, getOtherValue(), backend, subsEndpoints);
        CacheTestUtils.addItem(getKnownCacheId(), SUBSCRIBED_KEY, getSubscribedValue(), backend, subsEndpoints);

        // Assert
        for (int i = 0; i < streamingHelpers.length; i++) {
            var eventsFuture = subscription.get(i);
            Awaitility.await().atMost(60, TimeUnit.SECONDS).until(eventsFuture::isDone);

            var events = eventsFuture.join();
            MatcherAssert.assertThat(events.size(), Matchers.is(1));

            var event = events.get(0);
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event.itemKey(), Matchers.is(SUBSCRIBED_KEY));
            MatcherAssert.assertThat(event.itemValue(), Matchers.is(getSubscribedValue()));
        }
    }
}
