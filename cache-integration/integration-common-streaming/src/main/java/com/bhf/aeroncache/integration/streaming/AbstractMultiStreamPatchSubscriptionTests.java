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

/**
 * Verifies patch-mode subscriptions: a client that subscribes to a cache in patch mode (via the
 * {@code ?mode=patch} query parameter) receives a {@link CacheUpdateEvent.EventType#PATCH_ITEM}
 * event carrying the merged value when an entry is patched, and is not notified on plain adds.
 * <p>
 * Patch subscriptions are cache-only, so values are JSON documents.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamPatchSubscriptionTests {

    private static final String PATCH_KEY = "patchKey";
    private static final String INITIAL_VALUE = "{\"a\":1,\"b\":2}";
    private static final String PATCH = "{\"b\":3,\"c\":4}";
    private static final String MERGED_VALUE = "{\"a\":1,\"b\":3,\"c\":4}";

    private final StreamingHelper[] streamingHelpers;
    private final TestEndpointsProvider subsEndpoints;

    protected AbstractMultiStreamPatchSubscriptionTests(TestEndpointsProvider endpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        this.subsEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(getKnownCacheId(), backend, subsEndpoints);
    }

    protected abstract String getKnownCacheId();

    @Test
    @DisplayName("Should receive a PATCH_ITEM event carrying the merged value when subscribed in patch mode")
    @HappyPath
    void shouldReceivePatchItemEventWhenSubscribedInPatchMode(BackendTestResource backend) {
        // Arrange: seed the entry BEFORE subscribing - patch subscribers are not notified on adds.
        CacheTestUtils.addItem(getKnownCacheId(), PATCH_KEY, INITIAL_VALUE, backend, subsEndpoints);

        var subscription = StreamingHelperUtil.getPerStreamPatchEvents(streamingHelpers, backend, getKnownCacheId(), 1);

        // Act
        CacheTestUtils.patchItem(getKnownCacheId(), PATCH_KEY, PATCH, backend, subsEndpoints);

        // Assert
        for (int i = 0; i < streamingHelpers.length; i++) {
            var eventsFuture = subscription.get(i);
            Awaitility.await().atMost(60, TimeUnit.SECONDS).until(eventsFuture::isDone);

            var events = eventsFuture.join();
            MatcherAssert.assertThat(events.size(), Matchers.is(1));

            var event = events.get(0);
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.PATCH_ITEM));
            MatcherAssert.assertThat(event.itemKey(), Matchers.is(PATCH_KEY));
            MatcherAssert.assertThat(event.itemValue(), Matchers.is(MERGED_VALUE));
        }
    }
}
