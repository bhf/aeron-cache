package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.annotations.HappyPath;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.integration.BackendTestLauncher;
import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.integration.config.StreamingTestEndpointsProvider;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BackendTestLauncher.class)
public abstract class AbstractMultiStreamBulkOpsTests {

    private static final String KNOWN_CACHE_ID = "1";
    private static final String NON_SUBSCRIBED_CACHE_ID = "NotSubscribedCache";
    private static final String KNOWN_KEY = "SomeKey";
    private static final String KNOWN_VALUE = "SomeValue";

    private final StreamingHelper[] streamingHelpers;
    private final StreamingTestEndpointsProvider streamingEndpointsProvider;
    private TestEndpointsProvider bulkOpsEndpoints;

    protected AbstractMultiStreamBulkOpsTests(TestEndpointsProvider endpointsProvider, StreamingTestEndpointsProvider streamingTestEndpointsProvider, StreamingHelper... streamingHelpers) {
        this.streamingHelpers = streamingHelpers;
        this.streamingEndpointsProvider = streamingTestEndpointsProvider;
        bulkOpsEndpoints = endpointsProvider;
    }

    @BeforeAll
    void setup(BackendTestResource backend) {
        CacheTestUtils.createCache(KNOWN_CACHE_ID, backend, bulkOpsEndpoints);
        CacheTestUtils.createCache(NON_SUBSCRIBED_CACHE_ID, backend, bulkOpsEndpoints);
    }

    @Test
    @DisplayName("Should get streaming updates when adding multiple items in bulk for subscribed cache")
    @HappyPath
    void shouldGetStreamingUpdatesWhenAddingMultipleItemsForSubscribedCache(BackendTestResource backend) {
        // Arrange
        int numItems = 3;
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, streamingEndpointsProvider, KNOWN_CACHE_ID, numItems);

        List<CacheOperationRequest> operations = new ArrayList<>();
        int i=0;
        for (; i < numItems; i++) {
            operations.add(new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0,0,  "req-" + i, KNOWN_CACHE_ID, KNOWN_KEY + "-" + i, KNOWN_VALUE + "-" + i));
        }
        operations.add(new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0,0,  "req-" + i, NON_SUBSCRIBED_CACHE_ID, KNOWN_KEY + "-" + i, KNOWN_VALUE + "-" + i));
        var bulkRequest = new BulkCacheOpsRequest("bulk-request-1", operations);

        // Act
        CacheTestUtils.sendBulkRequest(bulkRequest, backend, bulkOpsEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();
            MatcherAssert.assertThat(streamingSourceEvents, Matchers.hasSize(numItems));

            for (int j = 0; j < numItems; j++) {
                var updateEvent = streamingSourceEvents.get(j);
                MatcherAssert.assertThat(updateEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
                MatcherAssert.assertThat(updateEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
                MatcherAssert.assertThat(updateEvent.itemKey(), Matchers.is(KNOWN_KEY + "-" + j));
                MatcherAssert.assertThat(updateEvent.itemValue(), Matchers.is(KNOWN_VALUE + "-" + j));
            }
        }
    }

    @Test
    @DisplayName("Should get streaming updates on mixed bulk operations")
    @HappyPath
    void shouldGetStreamingUpdatesOnMixedBulkOps(BackendTestResource backend) {
        // Arrange
        var testKey = "mixed-bulkops-key";
        var firstValue = "value-1";
        var secondValue = "value-2";
        int numItems = 4;
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, streamingEndpointsProvider, KNOWN_CACHE_ID, numItems);

        var operations = List.of(
                new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0, 0, UUID.randomUUID().toString(), KNOWN_CACHE_ID, testKey, firstValue),
                new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0, 0, UUID.randomUUID().toString(), KNOWN_CACHE_ID, testKey, secondValue),
                new CacheOperationRequest(BulkOperationType.REMOVE_ITEM, 0, 0, UUID.randomUUID().toString(), KNOWN_CACHE_ID, testKey, null),
                new CacheOperationRequest(BulkOperationType.CLEAR_CACHE, 0, 0, UUID.randomUUID().toString(), KNOWN_CACHE_ID, null, null)
        );
        var bulkRequest = new BulkCacheOpsRequest("bulk-request-mixed", operations);

        // Act
        CacheTestUtils.sendBulkRequest(bulkRequest, backend, bulkOpsEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();
            MatcherAssert.assertThat(streamingSourceEvents, Matchers.hasSize(numItems));

            // Assert for add item
            var addEvent = streamingSourceEvents.get(0);
            MatcherAssert.assertThat(addEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(addEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(addEvent.itemKey(), Matchers.is(testKey));
            MatcherAssert.assertThat(addEvent.itemValue(), Matchers.is(firstValue));

            // Assert for second add on same key
            var secondAddEvent = streamingSourceEvents.get(1);
            MatcherAssert.assertThat(secondAddEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(secondAddEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(secondAddEvent.itemKey(), Matchers.is(testKey));
            MatcherAssert.assertThat(secondAddEvent.itemValue(), Matchers.is(secondValue));

            // Assert for remove
            var removeEvent = streamingSourceEvents.get(2);
            MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(removeEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(removeEvent.itemKey(), Matchers.is(testKey));

            // Assert for clearing the cache
            var clearEvent = streamingSourceEvents.get(3);
            MatcherAssert.assertThat(clearEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.CLEAR_CACHE));
            MatcherAssert.assertThat(clearEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
        }
    }

    @Test
    @DisplayName("Should get remove event when item with ttl expires")
    @HappyPath
    void shouldGetRemoveEventWhenItemWithTtlExpires(BackendTestResource backend) {
        // Arrange
        var ttlKey = "ttl-key";
        var ttlValue = "ttl-value";
        long ttlMs = 1000;
        int numExpectedEvents = 2;
        var perStreamingSourceEvents = StreamingHelperUtil.getPerStreamEvents(streamingHelpers, backend, streamingEndpointsProvider, KNOWN_CACHE_ID, numExpectedEvents);

        var operations = List.of(
                new CacheOperationRequest(BulkOperationType.ADD_ITEM, ttlMs,0,  UUID.randomUUID().toString(), KNOWN_CACHE_ID, ttlKey, ttlValue)
        );
        var bulkRequest = new BulkCacheOpsRequest("bulk-request-ttl", operations);

        // Act
        CacheTestUtils.sendBulkRequest(bulkRequest, backend, bulkOpsEndpoints);

        // Assert
        for (var streamingSourceEventsFuture : perStreamingSourceEvents) {
            Awaitility.await()
                    .atMost(60, TimeUnit.SECONDS)
                    .until(streamingSourceEventsFuture::isDone);

            var streamingSourceEvents = streamingSourceEventsFuture.join();
            MatcherAssert.assertThat(streamingSourceEvents, Matchers.hasSize(numExpectedEvents));

            // Assert for add item
            var addEvent = streamingSourceEvents.get(0);
            MatcherAssert.assertThat(addEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.ADD_ITEM));
            MatcherAssert.assertThat(addEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(addEvent.itemKey(), Matchers.is(ttlKey));
            MatcherAssert.assertThat(addEvent.itemValue(), Matchers.is(ttlValue));

            // Assert for remove item (on expiration)
            var removeEvent = streamingSourceEvents.get(1);
            MatcherAssert.assertThat(removeEvent.eventType(), Matchers.is(CacheUpdateEvent.EventType.REMOVE_ITEM));
            MatcherAssert.assertThat(removeEvent.cacheId(), Matchers.is(KNOWN_CACHE_ID));
            MatcherAssert.assertThat(removeEvent.itemKey(), Matchers.is(ttlKey));
        }
    }

}
