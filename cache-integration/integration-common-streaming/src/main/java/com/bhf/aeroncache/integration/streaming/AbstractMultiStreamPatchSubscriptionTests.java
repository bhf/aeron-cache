package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.TestEndpointsProvider;
import com.bhf.aeroncache.integration.utils.CacheTestUtils;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Verifies patch-mode subscriptions: a client that subscribes to a cache in patch mode (via the
 * {@code ?mode=patch} query parameter) receives a {@link CacheUpdateEvent.EventType#PATCH_ITEM}
 * event carrying the JSON merge patch (the delta) when an entry is patched, or when an add
 * overwrites an existing entry. It is not notified on the initial add of a new key.
 * <p>
 * Patch subscriptions are cache-only, so values are JSON documents.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamPatchSubscriptionTests {

    private static final String PATCH_KEY = "patchKey";
    private static final String ADD_OVERWRITE_KEY = "addOverwriteKey";
    private static final String BULK_ADD_OVERWRITE_KEY = "bulkAddOverwriteKey";
    private static final String BULK_PATCH_KEY = "bulkPatchKey";
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
    @DisplayName("Should receive a PATCH_ITEM event carrying the patch delta when subscribed in patch mode")
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
            MatcherAssert.assertThat(event.itemValue(), Matchers.is(PATCH));
        }
    }

    @Test
    @DisplayName("Should receive a PATCH_ITEM event carrying the merge patch when an add overwrites an existing entry")
    @HappyPath
    void shouldReceivePatchItemEventWhenAddOverwritesExistingEntry(BackendTestResource backend) {
        // Arrange: seed the entry BEFORE subscribing - patch subscribers are not notified on the first add.
        CacheTestUtils.addItem(getKnownCacheId(), ADD_OVERWRITE_KEY, INITIAL_VALUE, backend, subsEndpoints);

        var subscription = StreamingHelperUtil.getPerStreamPatchEvents(streamingHelpers, backend, getKnownCacheId(), 1);

        // Act - overwrite the existing entry with a new value
        CacheTestUtils.addItem(getKnownCacheId(), ADD_OVERWRITE_KEY, MERGED_VALUE, backend, subsEndpoints);

        // Assert - the patch subscriber receives the computed merge patch (the delta)
        for (int i = 0; i < streamingHelpers.length; i++) {
            var eventsFuture = subscription.get(i);
            Awaitility.await().atMost(60, TimeUnit.SECONDS).until(eventsFuture::isDone);

            var events = eventsFuture.join();
            MatcherAssert.assertThat(events.size(), Matchers.is(1));

            var event = events.get(0);
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.PATCH_ITEM));
            MatcherAssert.assertThat(event.itemKey(), Matchers.is(ADD_OVERWRITE_KEY));
            MatcherAssert.assertThat(event.itemValue(), Matchers.is(PATCH));
        }
    }

    @Test
    @DisplayName("Should receive a PATCH_ITEM event carrying the merge patch when a bulk ADD_ITEM overwrites an existing entry")
    @HappyPath
    void shouldReceivePatchItemEventWhenBulkAddOverwritesExistingEntry(BackendTestResource backend) {
        // Arrange: seed the entry BEFORE subscribing - patch subscribers are not notified on the first add.
        CacheTestUtils.addItem(getKnownCacheId(), BULK_ADD_OVERWRITE_KEY, INITIAL_VALUE, backend, subsEndpoints);

        var subscription = StreamingHelperUtil.getPerStreamPatchEvents(streamingHelpers, backend, getKnownCacheId(), 1);

        // Act - overwrite the existing entry via a bulk ADD_ITEM operation
        var operations = List.of(
                new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0, 0, UUID.randomUUID().toString(), getKnownCacheId(), BULK_ADD_OVERWRITE_KEY, MERGED_VALUE)
        );
        CacheTestUtils.sendBulkRequest(new BulkCacheOpsRequest(UUID.randomUUID().toString(), operations), backend, subsEndpoints);

        // Assert - the patch subscriber receives the computed merge patch (the delta)
        for (int i = 0; i < streamingHelpers.length; i++) {
            var eventsFuture = subscription.get(i);
            Awaitility.await().atMost(60, TimeUnit.SECONDS).until(eventsFuture::isDone);

            var events = eventsFuture.join();
            MatcherAssert.assertThat(events.size(), Matchers.is(1));

            var event = events.get(0);
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.PATCH_ITEM));
            MatcherAssert.assertThat(event.itemKey(), Matchers.is(BULK_ADD_OVERWRITE_KEY));
            MatcherAssert.assertThat(event.itemValue(), Matchers.is(PATCH));
        }
    }

    @Test
    @DisplayName("Should receive a PATCH_ITEM event carrying the patch delta when an entry is patched via a bulk PATCH_ITEM")
    @HappyPath
    void shouldReceivePatchItemEventWhenPatchedViaBulkOp(BackendTestResource backend) {
        // Arrange: seed the entry BEFORE subscribing - patch subscribers are not notified on adds.
        CacheTestUtils.addItem(getKnownCacheId(), BULK_PATCH_KEY, INITIAL_VALUE, backend, subsEndpoints);

        var subscription = StreamingHelperUtil.getPerStreamPatchEvents(streamingHelpers, backend, getKnownCacheId(), 1);

        // Act - patch the entry via a bulk PATCH_ITEM operation
        var operations = List.of(
                new CacheOperationRequest(BulkOperationType.PATCH_ITEM, 0, 0, UUID.randomUUID().toString(), getKnownCacheId(), BULK_PATCH_KEY, PATCH)
        );
        CacheTestUtils.sendBulkRequest(new BulkCacheOpsRequest(UUID.randomUUID().toString(), operations), backend, subsEndpoints);

        // Assert - the patch subscriber receives the client patch delta
        for (int i = 0; i < streamingHelpers.length; i++) {
            var eventsFuture = subscription.get(i);
            Awaitility.await().atMost(60, TimeUnit.SECONDS).until(eventsFuture::isDone);

            var events = eventsFuture.join();
            MatcherAssert.assertThat(events.size(), Matchers.is(1));

            var event = events.get(0);
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.PATCH_ITEM));
            MatcherAssert.assertThat(event.itemKey(), Matchers.is(BULK_PATCH_KEY));
            MatcherAssert.assertThat(event.itemValue(), Matchers.is(PATCH));
        }
    }
}
