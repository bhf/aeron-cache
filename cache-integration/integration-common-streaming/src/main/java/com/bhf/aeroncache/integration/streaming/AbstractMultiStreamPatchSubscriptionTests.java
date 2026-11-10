package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.events.CacheUpdateEvent;
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
 * overwrites an existing entry. When the add is the first insert of a key (nothing to diff against),
 * the subscriber receives the whole value as an initial {@code ADD_ITEM} baseline, so a subscriber
 * that joined before the key existed is not left with a stale (empty) view. When a valid merge patch
 * cannot be computed (e.g. the stored or incoming value is not parseable JSON), the subscriber
 * likewise receives the whole new value as a full {@code ADD_ITEM} fallback rather than nothing, so
 * it does not silently retain stale state.
 * <p>
 * Patch subscriptions are cache-only, so values are JSON documents.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamPatchSubscriptionTests {

    private static final String BASELINE_KEY = "baselineKey";
    private static final String PATCH_KEY = "patchKey";
    private static final String ADD_OVERWRITE_KEY = "addOverwriteKey";
    private static final String BULK_ADD_OVERWRITE_KEY = "bulkAddOverwriteKey";
    private static final String BULK_PATCH_KEY = "bulkPatchKey";
    private static final String ADD_FALLBACK_KEY = "addFallbackKey";
    private static final String INITIAL_VALUE = "{\"a\":1,\"b\":2}";
    private static final String PATCH = "{\"b\":3,\"c\":4}";
    private static final String MERGED_VALUE = "{\"a\":1,\"b\":3,\"c\":4}";
    /** A value that is not parseable JSON, so no merge patch can be computed against the previous value. */
    private static final String UNPARSEABLE_VALUE = "not valid json";

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
    @DisplayName("Should receive a full ADD_ITEM baseline when a key is first added after subscribing in patch mode")
    @HappyPath
    void shouldReceiveBaselineAddItemOnFirstAddAfterSubscribing(BackendTestResource backend) {
        // Arrange: subscribe in patch mode BEFORE the key exists, so there is nothing in the cache to diff
        // against when it is first added.
        var subscription = StreamingHelperUtil.getPerStreamPatchEvents(streamingHelpers, backend, getKnownCacheId(), 1);

        // Act - first insert of a fresh key
        CacheTestUtils.addItem(getKnownCacheId(), BASELINE_KEY, INITIAL_VALUE, backend, subsEndpoints);

        // Assert - rather than being left with a stale (empty) view, the patch subscriber receives the whole
        // value as an initial baseline, delivered as a full ADD_ITEM (not a PATCH_ITEM delta).
        for (int i = 0; i < streamingHelpers.length; i++) {
            var eventsFuture = subscription.get(i);
            Awaitility.await().atMost(60, TimeUnit.SECONDS).until(eventsFuture::isDone);

            var events = eventsFuture.join();
            MatcherAssert.assertThat(events.size(), Matchers.is(1));

            var event = events.get(0);
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event.itemKey(), Matchers.is(BASELINE_KEY));
            MatcherAssert.assertThat(event.itemValue(), Matchers.is(INITIAL_VALUE));
        }
    }

    @Test
    @DisplayName("Should receive a PATCH_ITEM event carrying the patch delta when subscribed in patch mode")
    @HappyPath
    void shouldReceivePatchItemEventWhenSubscribedInPatchMode(BackendTestResource backend) {
        // Arrange: seed the entry BEFORE subscribing, so the subscriber is not present for the first-add
        // baseline and observes only the later patch.
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
        // Arrange: seed the entry BEFORE subscribing, so the subscriber is not present for the first-add
        // baseline and observes only the later overwrite.
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
    @DisplayName("Should receive a full ADD_ITEM carrying the whole value when a merge patch cannot be computed on overwrite")
    @HappyPath
    void shouldReceiveFullAddItemWhenMergePatchCannotBeComputed(BackendTestResource backend) {
        // Arrange: seed a valid JSON entry BEFORE subscribing, so the subscriber is not present for the
        // first-add baseline; the stored value must be parseable so the cluster has a baseline to diff against.
        CacheTestUtils.addItem(getKnownCacheId(), ADD_FALLBACK_KEY, INITIAL_VALUE, backend, subsEndpoints);

        var subscription = StreamingHelperUtil.getPerStreamPatchEvents(streamingHelpers, backend, getKnownCacheId(), 1);

        // Act - overwrite the existing entry with a value that is not parseable JSON, so the cluster cannot
        // compute a merge patch delta from the previous value.
        CacheTestUtils.addItem(getKnownCacheId(), ADD_FALLBACK_KEY, UNPARSEABLE_VALUE, backend, subsEndpoints);

        // Assert - rather than being left stale (no event at all), the patch subscriber receives the whole new
        // value as a full ADD_ITEM fallback, not a PATCH_ITEM delta.
        for (int i = 0; i < streamingHelpers.length; i++) {
            var eventsFuture = subscription.get(i);
            Awaitility.await().atMost(60, TimeUnit.SECONDS).until(eventsFuture::isDone);

            var events = eventsFuture.join();
            MatcherAssert.assertThat(events.size(), Matchers.is(1));

            var event = events.get(0);
            MatcherAssert.assertThat(event.cacheId(), Matchers.is(getKnownCacheId()));
            MatcherAssert.assertThat(event.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(event.itemKey(), Matchers.is(ADD_FALLBACK_KEY));
            MatcherAssert.assertThat(event.itemValue(), Matchers.is(UNPARSEABLE_VALUE));
        }
    }

    @Test
    @DisplayName("Should receive a PATCH_ITEM event carrying the merge patch when a bulk ADD_ITEM overwrites an existing entry")
    @HappyPath
    void shouldReceivePatchItemEventWhenBulkAddOverwritesExistingEntry(BackendTestResource backend) {
        // Arrange: seed the entry BEFORE subscribing, so the subscriber is not present for the first-add
        // baseline and observes only the later overwrite.
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
        // Arrange: seed the entry BEFORE subscribing, so the subscriber is not present for the first-add
        // baseline and observes only the later patch.
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
