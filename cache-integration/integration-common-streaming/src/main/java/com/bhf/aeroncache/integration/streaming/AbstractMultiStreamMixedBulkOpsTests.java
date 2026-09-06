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
 * Verifies that a single bulk ops request containing both cache (item) and counter operations
 * is fanned out correctly, with the cache stream receiving only the cache events and the counter
 * stream receiving only the counter events.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamMixedBulkOpsTests {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String KNOWN_COUNTER_CACHE_ID = "counter-1";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";
    private static final Integer KNOWN_COUNTER_VALUE = 3;

    private final StreamingHelper[] cacheStreamingHelpers;
    private final StreamingHelper[] counterStreamingHelpers;
    private final TestEndpointsProvider cacheEndpoints;
    private final TestEndpointsProvider counterEndpoints;

    protected AbstractMultiStreamMixedBulkOpsTests(TestEndpointsProvider cacheEndpoints,
                                                   TestEndpointsProvider counterEndpoints,
                                                   StreamingHelper[] cacheStreamingHelpers,
                                                   StreamingHelper[] counterStreamingHelpers) {
        this.cacheEndpoints = cacheEndpoints;
        this.counterEndpoints = counterEndpoints;
        this.cacheStreamingHelpers = cacheStreamingHelpers;
        this.counterStreamingHelpers = counterStreamingHelpers;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, cacheEndpoints);
        CacheTestUtils.createCache(KNOWN_COUNTER_CACHE_ID, backend, counterEndpoints);
    }

    @Test
    @DisplayName("Should get streaming updates on both cache and counter streams for a single mixed bulk request")
    @HappyPath
    void shouldGetStreamingUpdatesForMixedCacheAndCounterBulkOps(BackendTestResource backend) {
        // Arrange
        int numCacheEvents = 3;   // add item, remove item, clear cache
        int numCounterEvents = 3; // add counter, remove counter, clear counter cache

        var perCacheStreamEvents = StreamingHelperUtil.getPerStreamEvents(cacheStreamingHelpers, backend, KNOWN_CACHE_ID, numCacheEvents);
        var perCounterStreamEvents = StreamingHelperUtil.getPerStreamEvents(counterStreamingHelpers, backend, KNOWN_COUNTER_CACHE_ID, numCounterEvents);

        var operations = List.of(
                new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0, 0, UUID.randomUUID().toString(), KNOWN_CACHE_ID, KNOWN_KEY, KNOWN_VALUE),
                new CacheOperationRequest(BulkOperationType.ADD_COUNTER, 0, KNOWN_COUNTER_VALUE, UUID.randomUUID().toString(), KNOWN_COUNTER_CACHE_ID, KNOWN_KEY, ""),
                new CacheOperationRequest(BulkOperationType.REMOVE_ITEM, 0, 0, UUID.randomUUID().toString(), KNOWN_CACHE_ID, KNOWN_KEY, null),
                new CacheOperationRequest(BulkOperationType.REMOVE_COUNTER, 0, 0, UUID.randomUUID().toString(), KNOWN_COUNTER_CACHE_ID, KNOWN_KEY, null),
                new CacheOperationRequest(BulkOperationType.CLEAR_CACHE, 0, 0, UUID.randomUUID().toString(), KNOWN_CACHE_ID, null, null),
                new CacheOperationRequest(BulkOperationType.CLEAR_COUNTER_CACHE, 0, 0, UUID.randomUUID().toString(), KNOWN_COUNTER_CACHE_ID, null, null)
        );
        var bulkRequest = new BulkCacheOpsRequest("bulk-request-mixed-cache-counter", operations);

        // Act
        CacheTestUtils.sendBulkRequest(bulkRequest, backend, cacheEndpoints);

        // Assert cache stream received only the cache events, in order
        for (var streamingSourceEventsFuture : perCacheStreamEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();
            MatcherAssert.assertThat(streamingSourceEvents, Matchers.hasSize(numCacheEvents));

            var addEvent = streamingSourceEvents.get(0);
            MatcherAssert.assertThat(addEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(addEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(addEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(addEvent.itemValue(), Matchers.is(KNOWN_VALUE));

            var removeEvent = streamingSourceEvents.get(1);
            MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(removeEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(removeEvent.itemKey(), Matchers.is(KNOWN_KEY));

            var clearEvent = streamingSourceEvents.get(2);
            MatcherAssert.assertThat(clearEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.CLEAR_CACHE));
            MatcherAssert.assertThat(clearEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
        }

        // Assert counter stream received only the counter events, in order
        for (var streamingSourceEventsFuture : perCounterStreamEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();
            MatcherAssert.assertThat(streamingSourceEvents, Matchers.hasSize(numCounterEvents));

            var addEvent = streamingSourceEvents.get(0);
            MatcherAssert.assertThat(addEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(addEvent.cacheId(), Matchers.is(KNOWN_COUNTER_CACHE_ID));
            MatcherAssert.assertThat(addEvent.itemKey(), Matchers.is(KNOWN_KEY));
            MatcherAssert.assertThat(addEvent.itemValue(), Matchers.is(KNOWN_COUNTER_VALUE));

            var removeEvent = streamingSourceEvents.get(1);
            MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(removeEvent.cacheId(), Matchers.is(KNOWN_COUNTER_CACHE_ID));
            MatcherAssert.assertThat(removeEvent.itemKey(), Matchers.is(KNOWN_KEY));

            var clearEvent = streamingSourceEvents.get(2);
            MatcherAssert.assertThat(clearEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.CLEAR_CACHE));
            MatcherAssert.assertThat(clearEvent.cacheId(), Matchers.is(KNOWN_COUNTER_CACHE_ID));
        }
    }

}
